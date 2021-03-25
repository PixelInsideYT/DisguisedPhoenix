package disuguisedphoenix;

import disuguisedphoenix.adder.EntityAdder;
import disuguisedphoenix.terrain.Island;
import disuguisedphoenix.terrain.PopulatedIsland;
import disuguisedphoenix.terrain.World;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;
import engine.collision.CollisionShape;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.ModelFileHandler;
import engine.util.Zeitgeist;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.context.Display;
import graphics.gui.NuklearBinding;
import graphics.loader.TextureLoader;
import graphics.objects.FrameBufferObject;
import graphics.objects.OpenGLState;
import graphics.objects.TimerQuery;
import graphics.objects.Vao;
import graphics.occlusion.SSAOEffect;
import graphics.occlusion.ShadowEffect;
import graphics.particles.ParticleManager;
import graphics.particles.PointSeekingEmitter;
import graphics.postprocessing.*;
import graphics.renderer.MultiIndirectRenderer;
import graphics.renderer.TestRenderer;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import graphics.world.Model;
import graphics.world.RenderInfo;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.MemoryStack;

import java.lang.Math;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL20.*;

public class Main {

    public static int inViewVerticies = 0;
    public static int inViewObjects = 0;
    public static int drawCalls = 0;
    public static int facesDrawn = 0;

    private static Model model;

    private static List<Entity> worldsEntity = new ArrayList<>();
    public static float scale = 60000;
    public static float radius = (float) Math.sqrt(Math.pow(scale / 2f, 2) * 3);

    //get models on itch and cgtrader

    private static List<CollisionShape> worldTris = new ArrayList<>();

    //NEXT Commit:
    // - improved free flight cam to support round world
    // - improved entity adder
    private static float lightSpeed = 0.25f;
    private static float lightAngle = 0;
    private static float lightRadius = 2 * radius;
    private static Vector3f lightPos = new Vector3f(0, 1, 0);
    private static Vector3f lightColor = new Vector3f();

    public static void main(String[] args) {
        // ModelFileHandler.regenerateModels("/home/linus/IdeaProjects/DisguisedPhoenix/src/main/resources/models/ModelBuilder.info");
        //TODO: refactor rendering into a modular pipeline
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 480, 360);
        display.setClearColor(new Vector3f(0f));
        GL11.glEnable(GL45.GL_DEBUG_OUTPUT);
        GLUtil.setupDebugMessageCallback();
        model = createSphere();
        MouseInputMap mim = new MouseInputMap();
        ParticleManager pm = new ParticleManager();
        MultiIndirectRenderer multiRenderer = new MultiIndirectRenderer();
        ModelFileHandler.loadModelsForMultiDraw(multiRenderer.persistantMatrixVbo, "plants/flowerTest1.modelFile", "lowPolyTree/vc.modelFile", "lowPolyTree/ballTree.modelFile", "lowPolyTree/bendyTree.modelFile", "lowPolyTree/tree2.modelFile", "misc/rock.modelFile", "plants/grass.modelFile", "plants/mushroom.modelFile", "misc/tutorialCrystal.modelFile", "plants/glockenblume.modelFile", "misc/fox.modelFile");
        World world = new World(pm);
        EntityAdder ea = new EntityAdder(pm);
        worldsEntity.addAll(ea.getAllEntities(new PopulatedIsland(new Vector3f(0), 1000)));
        worldsEntity.forEach(e -> placeEntity(e));
        //  for (int i = 0; i < 1; i++) world.addIsland(1000);
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, scale + 100, 0), mim);
        ShaderFactory shaderFactory = new ShaderFactory("testVSMultiDraw.glsl", "testFS.glsl").withAttributes("posAndWobble", "colorAndShininess");
        shaderFactory.withUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "transformationMatrixUniform", "useInputTransformationMatrix");
        Shader shader = shaderFactory.configureSampler("noiseMap", 0).built();
        int noiseTexture = TextureLoader.loadTexture("misc/noiseMap.png", GL_REPEAT, GL_LINEAR);
        TestRenderer renderer = new TestRenderer(shader);
        Matrix4f projMatrix = new Matrix4f();
        int width = display.getWidth();
        int height = display.getHeight();
        float aspectRatio = width / (float) height;
        projMatrix.perspective((float) Math.toRadians(70), aspectRatio, 1f, 100000);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW_KEY_W).addMapping("backward", GLFW_KEY_S).addMapping("turnLeft", GLFW_KEY_A).addMapping("turnRight", GLFW_KEY_D).addMapping("accel", GLFW_KEY_SPACE);
        KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW_KEY_W).addMapping("backward", GLFW_KEY_S).addMapping("goLeft", GLFW_KEY_A).addMapping("goRight", GLFW_KEY_D).addMapping("up", GLFW_KEY_SPACE).addMapping("down", GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight", GLFW_KEY_LEFT_CONTROL);
        input.addInputMapping(kim);
        input.addInputMapping(freeFlightCam);
        input.addInputMapping(mim);
        FreeFlightCamera flightCamera = new FreeFlightCamera(mim, freeFlightCam);
        player.movement = kim;
        OpenGLState.enableDepthTest();
        OpenGLState.setAlphaBlending(GL_ONE_MINUS_SRC_ALPHA);
        System.err.println("STARTUP TIME: " + (System.currentTimeMillis() - startUpTime) / 1000f);
        //  display.activateWireframe();
        Zeitgeist zeitgeist = new Zeitgeist();
        boolean wireframe = false;
        boolean collisionBoxes = false;
        boolean freeFlightCamActivated = false;
        long lastSwitchWireframe = System.currentTimeMillis();
        long lastSwitchCollision = System.currentTimeMillis();
        long lastSwitchCam = System.currentTimeMillis();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        float time = 1f;
        DecimalFormat df = new DecimalFormat("###,###,###");
        FrameBufferObject fbo = new FrameBufferObject(width, height, 2)
                //normal and specular
                .addTextureAttachment(0)
                //color and geometry info
                .addTextureAttachment(1)
                //depth
                .addDepthTextureAttachment(true);
        FrameBufferObject deferredResult = new FrameBufferObject(width, height, 2).addTextureAttachment(0).addTextureAttachment(1).unbind();
        QuadRenderer quadRenderer = new QuadRenderer();
        GaussianBlur blur = new GaussianBlur(quadRenderer);
        SSAOEffect ssao = new SSAOEffect(quadRenderer, width, height, projMatrix);
        ShadowEffect shadows = new ShadowEffect();
        HIZGenerator hizGen = new HIZGenerator(quadRenderer);
        Pipeline postProcessPipeline = new Pipeline(width, height, projMatrix, quadRenderer, blur);
        float avgFPS = 0;
        int frameCounter = 0;
        display.setResizeListener((width1, height1, aspectRatio1) -> {
            projMatrix.identity().perspective((float) Math.toRadians(70), aspectRatio1, 1f, 100000);
            fbo.resize(width1, height1);
            deferredResult.resize(width1, height1);
            ssao.resize(width1, height1);
            postProcessPipeline.resize(width1, height1);
        });
        TimerQuery vertexTimer = new TimerQuery("Geometry Pass");
        TimerQuery lightTimer = new TimerQuery("Lighting Pass");
        Matrix4f cullingMatrix = new Matrix4f();
        FrustumIntersection cullingHelper = new FrustumIntersection();
        NuklearBinding nuklearBinding=new NuklearBinding(input,display);
        nuklearBinding.registerGui(postProcessPipeline.getAtmosphere());
        while (!display.shouldClose() && !input.isKeyDown(GLFW_KEY_ESCAPE)) {
            float dt = zeitgeist.getDelta();
           // lightAngle += lightSpeed * dt;
           lightAngle=1f;
            lightPos.z = (float) Math.cos(lightAngle) * lightRadius;
            lightPos.y = (float) Math.sin(lightAngle) * lightRadius;
            long startFrame = System.nanoTime();
            time += dt;
            display.pollEvents();
           // nuklearBinding.pollInputs();
            if (input.isKeyDown(GLFW_KEY_O) && System.currentTimeMillis() - lastSwitchWireframe > 100) {
                wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
                //world.addNextEntities(player.position);
                //world.addAllEntities();
            }
            if (input.isKeyDown(GLFW_KEY_P)) {
                PointSeekingEmitter entitySeeker = new PointSeekingEmitter(player.position, new Vector3f(player.position).add(new Vector3f(player.forward).mul(700)), 15, 700f, 30, world);
                pm.addParticleEmitter(entitySeeker);
            }
            if (input.isKeyDown(GLFW_KEY_L) && System.currentTimeMillis() - lastSwitchCollision > 100) {
                collisionBoxes = !collisionBoxes;
                lastSwitchCollision = System.currentTimeMillis();
            }
            if (input.isKeyDown(GLFW_KEY_C) && System.currentTimeMillis() - lastSwitchCam > 100) {
                freeFlightCamActivated = !freeFlightCamActivated;
                if (freeFlightCamActivated) input.hideMouseCursor();
                else input.showMouseCursor();
                lastSwitchCam = System.currentTimeMillis();
            }
            if (wireframe) {
                OpenGLState.enableWireframe();
            } else {
                OpenGLState.disableWireframe();
            }
            pm.update(dt);
            Camera ffc = player.cam;
            if (!freeFlightCamActivated) {
                //player.move(world.getPossibleTerrainCollisions(player), dt, world.getPossibleCollisions(player));
                player.move(world.getPossibleTerrainCollisions(player), dt, worldsEntity, worldTris);
                flightCamera.getPosition().set(player.cam.getPosition());
            } else {
                ffc = flightCamera;
            }
            world.update(dt);
            ffc.update(dt);
            input.updateInputMaps();
            Matrix4f viewMatrix = ffc.getViewMatrix();
            vertexTimer.startQuery();
            OpenGLState.enableBackFaceCulling();
            OpenGLState.enableDepthTest();
            OpenGLState.disableAlphaBlending();
            fbo.bind();
            glClearColor(0f, 0f, 0f, 0.0f);
            fbo.clear();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, noiseTexture);
            world.renderAdder(projMatrix, viewMatrix);
            renderer.begin(viewMatrix, projMatrix);
            shader.loadInt("useInputTransformationMatrix", 0);
            shader.loadFloat("time", time);
            renderer.render(model, new Matrix4f());
            renderer.render(player.getModel(), player.getTransformationMatrix());
            world.render(renderer, projMatrix, viewMatrix, ffc.getPosition());
            shader.loadInt("useInputTransformationMatrix", 1);
            cullingHelper.set(cullingMatrix.set(projMatrix).mul(viewMatrix));
            multiRenderer.prepareRenderer(world.getVisibleEntities(projMatrix, viewMatrix, ffc.getPosition()));
            multiRenderer.render();
            multiRenderer.prepareRenderer(worldsEntity);
            multiRenderer.render();
            fbo.unbind();
            shadows.render(viewMatrix,(float) Math.toRadians(70), aspectRatio,lightPos,shader,multiRenderer);
            vertexTimer.waitOnQuery();
            OpenGLState.enableAlphaBlending();
            display.clear();
            OpenGLState.disableWireframe();
            hizGen.generateHiZMipMap(fbo);
            OpenGLState.disableDepthTest();
           /* computeTimer.startQuery();
            postProcessPipeline.computeDOFEffect(fbo.getDepthTexture());
            computeTimer.waitOnQuery();*/
            fbo.blitDepth(deferredResult);
            ssao.renderEffect(fbo, projMatrix);
            lightTimer.startQuery();
            deferredResult.bind();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, fbo.getDepthTexture());
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, fbo.getTextureID(0));
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, fbo.getTextureID(1));
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, ssao.getSSAOTexture());
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, shadows.getShadowTexture());
            lightColor = postProcessPipeline.calculateLightColor(lightPos, ffc.getPosition());
            lightColor.set(1f);
            quadRenderer.renderDeferredLightingPass(ffc.getViewMatrix(), projMatrix, lightPos, lightColor, ssao.isEnabled(),shadows.getShadowProjViewMatrix());
            OpenGLState.enableDepthTest();
            OpenGLState.enableAlphaBlending();
            pm.render(projMatrix, viewMatrix);
            OpenGLState.disableAlphaBlending();
            OpenGLState.disableDepthTest();
            deferredResult.unbind();
            lightTimer.waitOnQuery();
            postProcessPipeline.applyPostProcessing(display, viewMatrix, deferredResult, fbo, lightPos, ffc);
         /*  OpenGLState.enableWireframe();
            shader.bind();
            shader.loadInt("useInputTransformationMatrix", 0);
            worldsEntity.forEach(entity -> {
                Collider collider = entity.getCollider();
                if (collider != null) {
                    collider.allTheShapes.stream().forEach(i -> {
                        if (i instanceof ConvexShape) {
                            ConvexShape cs = (ConvexShape) i;
                            if (cs.canBeRenderd()) {
                                Vao toRender = cs.getModel();
                                toRender.bind();
                                shader.loadMatrix("transformationMatrixUniform", cs.getTransformation());
                                GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                                toRender.unbind();
                            }
                        }
                    });
                }
            });
            shader.unbind();
            OpenGLState.disableWireframe();*/
            nuklearBinding.renderGUI(display.getWidth(),display.getHeight());
            display.flipBuffers();
            avgFPS += zeitgeist.getFPS();
            display.setFrameTitle("Disguised Phoenix: " + " FPS: " + zeitgeist.getFPS() + ", In frustum objects: " + inViewObjects + ", drawcalls: " + drawCalls + " faces: " + df.format(facesDrawn));
            inViewObjects = 0;
            inViewVerticies = 0;
            facesDrawn = 0;
            drawCalls = 0;
            zeitgeist.sleep();
            frameCounter++;
        }
        vertexTimer.printResults();
        lightTimer.printResults();
        if (ssao.isEnabled())
            ssao.ssaoTimer.printResults();
        nuklearBinding.cleanUp();
        postProcessPipeline.printTimers();
        System.out.println("AVG FPS: " + (avgFPS / (float) frameCounter));
        TextureLoader.cleanUpAllTextures();
        FrameBufferObject.cleanUpAllFbos();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

    private static Entity generateActivationSwitch(Island terrain) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * terrain.getSize() + terrain.position.x;
        float z = rnd.nextFloat() * terrain.getSize() + terrain.position.z;
        float h = terrain.getHeightOfTerrain(x, terrain.position.y, z);
        return new RotatingEntity(ModelFileHandler.getModel("cube.modelFile"), x, h + 50, z, 40);
    }

    private static String format(float v, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f", v);
    }

    private static float noiseScale = 1f / scale;
    private static float change = 0.05f;

    private static void placeEntity(Entity e) {
        Vector3f pos = e.position;
        Random r = new Random();

        float radius = (float) Math.sqrt(Math.pow(scale / 2f, 2) * 3);
        pos.set(r.nextFloat() * 2f - 1f, r.nextFloat() * 0.1f + 0.9f, r.nextFloat() * 2f - 1f).normalize(radius);
        pos.mul(1 + SimplexNoise.noise(pos.x * noiseScale, pos.y * noiseScale, pos.z * noiseScale) * change);
        Vector3f eulerAngles = new Vector3f();
        Quaternionf qf = new Quaternionf();
        qf.rotateTo(new Vector3f(0, 1, 0), new Vector3f(pos).normalize());
        qf.mul(new Quaternionf().rotateLocalY(r.nextFloat() * 7f), qf);
        qf.getEulerAnglesXYZ(eulerAngles);
        e.rotX = eulerAngles.x;
        e.rotY = eulerAngles.y;
        e.rotZ = eulerAngles.z;
    }


    private static Model createSphere() {
        int subdivisions = 25;
        List<Vector3f> sphereVerticies = createPlane(subdivisions, scale, 1, 0, 0);
        int indicyOffset = sphereVerticies.size();
        sphereVerticies.addAll(createPlane(subdivisions, scale, -1, 0, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, -1, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 1, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 0, 1));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 0, -1));

        float radius = (float) Math.sqrt(Math.pow(scale / 2f, 2) * 3);
        sphereVerticies.forEach(v -> v.normalize(radius));
        sphereVerticies.forEach(v -> v.mul(1 + SimplexNoise.noise(v.x * noiseScale, v.y * noiseScale, v.z * noiseScale) * change));
        List<Integer> sphereIndicies = createIndiecies(subdivisions, 0, true);
        for (int i = 1; i < 6; i++) {
            sphereIndicies.addAll(createIndiecies(subdivisions, indicyOffset * i, i % 2 == 0));
        }
        Vector3f colorVec = new Vector3f(0.1f, 0.6f, 0.1f);
        float[] v = new float[sphereVerticies.size() * 4];
        float[] color = new float[sphereVerticies.size() * 4];
        int pointer = 0;
        int colorPointer = 0;
        //create world triangle collisions
        for (int i = 0; i < sphereIndicies.size(); i += 3) {
            Vector3f v1 = sphereVerticies.get(sphereIndicies.get(i));
            Vector3f v2 = sphereVerticies.get(sphereIndicies.get(i + 1));
            Vector3f v3 = sphereVerticies.get(sphereIndicies.get(i + 2));
            Vector3f v1ToV2 = new Vector3f(v2).sub(v1);
            Vector3f v1ToV3 = new Vector3f(v3).sub(v1);
            Vector3f normal = v1ToV3.cross(v1ToV2).normalize();
            worldTris.add(new CollisionShape(new Vector3f[]{v1, v2, v3}, new Vector3f[]{normal}));
        }
        for (Vector3f vec : sphereVerticies) {
            v[pointer++] = vec.x;
            v[pointer++] = vec.y;
            v[pointer++] = vec.z;
            v[pointer++] = 0f;
            color[colorPointer++] = colorVec.x;
            color[colorPointer++] = colorVec.y;
            color[colorPointer++] = colorVec.z;
            color[colorPointer++] = 0;
        }
        int[] plane1Indicies = sphereIndicies.stream().mapToInt(i -> i).toArray();
        Vao vao = new Vao();
        vao.addDataAttributes(0, 4, v);
        vao.addDataAttributes(1, 4, color);
        vao.addIndicies(plane1Indicies);
        return new Model(new RenderInfo(vao), null, 0, 0, 0, null);
    }


    private static List<Vector3f> createPlane(int supdivisions, float scale, int xz, int yz, int xy) {
        List<Vector3f> rt = new ArrayList<>();
        float half = scale / 2f;
        float unit = 1f / (1f + supdivisions);
        for (int x = 0; x < 2 + supdivisions; x++) {
            for (int y = 0; y < 2 + supdivisions; y++) {
                float xx, yy, zz;
                xx = yy = zz = 0;
                //xz
                xx += (x * unit * scale - half) * xz;
                yy += half * xz;
                zz += (y * unit * scale - half) * xz;
                //yz
                yy += (x * unit * scale - half) * yz;
                xx += half * yz;
                zz += (y * unit * scale - half) * yz;
                //xy
                yy += (x * unit * scale - half) * xy;
                zz += half * xy;
                xx += (y * unit * scale - half) * xy;

                Vector3f v = new Vector3f(xx, yy, zz);
                rt.add(v);
            }
        }
        return rt;
    }

    private static List<Integer> createIndiecies(int subdivisions, int offset, boolean flip) {
        List<Integer> indicies = new ArrayList<>();
        int vertexCount = 2 + subdivisions;
        for (int z = 0; z < 2 + subdivisions - 1; z++) {
            for (int x = 0; x < 2 + subdivisions - 1; x++) {
                int topLeft = x + z * vertexCount + offset;
                int topRight = topLeft + 1;
                int botLeft = topLeft + vertexCount;
                int botRight = topRight + vertexCount;
                if (flip) indicies.add(botLeft);
                indicies.add(topLeft);
                if (!flip) indicies.add(botLeft);
                indicies.add(topRight);
                if (flip) indicies.add(botLeft);
                indicies.add(topRight);
                if (!flip) indicies.add(botLeft);
                indicies.add(botRight);
            }
        }
        return indicies;
    }
}
