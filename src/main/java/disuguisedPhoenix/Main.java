package disuguisedPhoenix;

import disuguisedPhoenix.adder.EntityAdder;
import disuguisedPhoenix.terrain.Island;
import disuguisedPhoenix.terrain.PopulatedIsland;
import disuguisedPhoenix.terrain.World;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.Maths;
import engine.util.ModelFileHandler;
import engine.util.Zeitgeist;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.context.Display;
import graphics.loader.TextureLoader;
import graphics.objects.FrameBufferObject;
import graphics.objects.OpenGLState;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.particles.ParticleManager;
import graphics.postProcessing.GaussianBlur;
import graphics.postProcessing.Pipeline;
import graphics.postProcessing.QuadRenderer;
import graphics.postProcessing.SSAOEffect;
import graphics.renderer.MultiIndirectRenderer;
import graphics.renderer.TestRenderer;
import graphics.world.Model;
import graphics.world.RenderInfo;
import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.awt.*;
import java.lang.Math;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static int inViewVerticies = 0;
    public static int inViewObjects = 0;
    public static int drawCalls = 0;
    public static int facesDrawn = 0;

    private static Model model;

    private static List<Entity> worldsEntity = new ArrayList<>();
    private static float scale = 10000;

    public static void main(String[] args) {
        //ModelFileHandler.regenerateModels("/home/linus/IdeaProjects/DisguisedPhoenix/src/main/resources/models/ModelBuilder.info");
        //TODO: refactor rendering into a modular pipeline
        Vector3f lightPos = new Vector3f(0, 10000, 0);
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 1920, 1080);
        Color sRGBClearColor = new Color(59, 168, 198);
        Vector3f linearClearColor = new Vector3f((float) Math.pow(sRGBClearColor.getRed() / 255f, 2.2), (float) Math.pow(sRGBClearColor.getGreen() / 255f, 2.2), (float) Math.pow(sRGBClearColor.getBlue() / 255f, 2.2));
        display.setClearColor(linearClearColor);
        GL11.glEnable(GL45.GL_DEBUG_OUTPUT);
        //GLUtil.setupDebugMessageCallback();
        int[] flags = new int[1];
        GL11.glGetIntegerv(GL45.GL_CONTEXT_FLAGS, flags);
        if ((flags[0] & GL45.GL_CONTEXT_FLAG_DEBUG_BIT) != 0) System.out.println("we have a debug context");
        else
            System.err.println("we DONT have debug context");
        model = createSphere();
        MouseInputMap mim = new MouseInputMap();
        ParticleManager pm = new ParticleManager();
        MultiIndirectRenderer multiRenderer = new MultiIndirectRenderer();
        ModelFileHandler.loadModelsForMultiDraw(multiRenderer.persistantMatrixVbo, "plants/flowerTest1.modelFile", "lowPolyTree/vc.modelFile", "lowPolyTree/ballTree.modelFile", "lowPolyTree/bendyTree.modelFile", "lowPolyTree/tree2.modelFile", "misc/rock.modelFile", "plants/grass.modelFile", "plants/mushroom.modelFile", "misc/tutorialCrystal.modelFile", "plants/glockenblume.modelFile", "misc/fox.modelFile");
        World world = new World(pm);
        EntityAdder ea = new EntityAdder(null);
        worldsEntity.addAll(ea.getAllEntities(new PopulatedIsland(new Vector3f(0),10000)));
        worldsEntity.forEach(e->placeEntity(e));
        //  for (int i = 0; i < 1; i++) world.addIsland(1000);
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, scale+100, 0), mim);
        Shader shader = new Shader(Shader.loadShaderCode("testVSMultiDraw.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("posAndWobble", "colorAndShininess");
        shader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "transformationMatrixUniform", "useInputTransformationMatrix");
        shader.connectSampler("noiseMap", 0);
        int noiseTexture = TextureLoader.loadTexture("misc/noiseMap.png", GL30.GL_REPEAT, GL30.GL_LINEAR);
        TestRenderer renderer = new TestRenderer(shader);
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective((float) Math.toRadians(70),/*0.8136752f*/16f / 9f, 1f, 100000);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("turnLeft", GLFW.GLFW_KEY_A).addMapping("turnRight", GLFW.GLFW_KEY_D).addMapping("accel", GLFW.GLFW_KEY_SPACE);
        KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("goLeft", GLFW.GLFW_KEY_A).addMapping("goRight", GLFW.GLFW_KEY_D).addMapping("up", GLFW.GLFW_KEY_SPACE).addMapping("down", GLFW.GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight", GLFW.GLFW_KEY_LEFT_CONTROL);
        input.addInputMapping(kim);
        input.addInputMapping(freeFlightCam);
        input.addInputMapping(mim);
        FreeFlightCamera flightCamera = new FreeFlightCamera(mim, freeFlightCam);
        player.movement = kim;
        OpenGLState.enableDepthTest();
        OpenGLState.setAlphaBlending(GL20.GL_ONE_MINUS_SRC_ALPHA);
        System.err.println("STARTUP TIME: " + (System.currentTimeMillis() - startUpTime) / 1000f);
        //  display.activateWireframe();
        world.addAllEntities();
        Zeitgeist zeitgeist = new Zeitgeist();
        boolean wireframe = false;
        boolean collisionBoxes = false;
        boolean freeFlightCamActivated = true;
        long lastSwitchWireframe = System.currentTimeMillis();
        long lastSwitchCollision = System.currentTimeMillis();
        long lastSwitchCam = System.currentTimeMillis();
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, noiseTexture);
        float time = 0f;
        input.hideMouseCursor();
        DecimalFormat df = new DecimalFormat("###,###,###");
        FrameBufferObject fbo = new FrameBufferObject(1920, 1080, 2)
                //normal and specular
                .addTextureAttachment(GL40.GL_RGB8, GL11.GL_UNSIGNED_BYTE, GL40.GL_RGB, 0)
                //color and geometry info
                .addTextureAttachment(1)
                //depth
                .addDepthTextureAttachment();
        FrameBufferObject deferredResult = new FrameBufferObject(1920, 1080, 2).addUnclampedTexture(0).addUnclampedTexture(1).addDepthBuffer().unbind();
        QuadRenderer quadRenderer = new QuadRenderer();
        GaussianBlur blur = new GaussianBlur(quadRenderer);
        SSAOEffect ssao = new SSAOEffect(quadRenderer, 1920, 1080, projMatrix);
        Pipeline postProcessPipeline = new Pipeline(1920, 1080, projMatrix, quadRenderer, blur);

        while (!display.shouldClose()) {
            float dt = zeitgeist.getDelta();
            long startFrame = System.nanoTime();
            time += dt;
            display.pollEvents();
            if (input.isKeyDown(GLFW.GLFW_KEY_O) && System.currentTimeMillis() - lastSwitchWireframe > 100) {
                wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
                world.addNextEntities(player.position);
                //world.addAllEntities();
            }
            if (input.isKeyDown(GLFW.GLFW_KEY_L) && System.currentTimeMillis() - lastSwitchCollision > 100) {
                collisionBoxes = !collisionBoxes;
                lastSwitchCollision = System.currentTimeMillis();
            }
            if (input.isKeyDown(GLFW.GLFW_KEY_C) && System.currentTimeMillis() - lastSwitchCam > 100) {
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
                player.move(world.getPossibleTerrainCollisions(player), dt, world.getPossibleCollisions(player));
                flightCamera.position.set(player.cam.position);
            } else {
                ffc = flightCamera;
            }
            world.update(dt);
            ffc.update(dt);
            input.updateInputMaps();
            Matrix4f viewMatrix = ffc.getViewMatrix();
            OpenGLState.enableBackFaceCulling();
            OpenGLState.enableDepthTest();
            OpenGLState.disableAlphaBlending();
            fbo.bind();
            GL30.glClearColor(linearClearColor.x, linearClearColor.y, linearClearColor.z, 0.0f);
            fbo.clear();
            GL30.glActiveTexture(GL30.GL_TEXTURE0);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, noiseTexture);
            world.renderAdder(projMatrix, viewMatrix);
            renderer.begin(viewMatrix, projMatrix);
            shader.loadInt("useInputTransformationMatrix", 0);
            shader.loadFloat("time", time);
            renderer.render(model, new Matrix4f());
            renderer.render(player.getModel(), player.getTransformationMatrix());
            world.render(renderer, projMatrix, viewMatrix, ffc.position);
            shader.loadInt("useInputTransformationMatrix", 1);
            multiRenderer.render(world.getVisibleEntities(projMatrix, viewMatrix, ffc.position));
            multiRenderer.render(worldsEntity);
            fbo.unbind();
            OpenGLState.enableAlphaBlending();
            display.clear();
            OpenGLState.disableWireframe();
            OpenGLState.disableDepthTest();
           /* computeTimer.startQuery();
            postProcessPipeline.computeDOFEffect(fbo.getDepthTexture());
            computeTimer.waitOnQuery();*/
            fbo.blitDepth(deferredResult);
            ssao.renderEffect(fbo);
            deferredResult.bind();
            GL30.glActiveTexture(GL30.GL_TEXTURE0);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, fbo.getDepthTexture());
            GL30.glActiveTexture(GL30.GL_TEXTURE1);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, fbo.getTextureID(0));
            GL30.glActiveTexture(GL30.GL_TEXTURE2);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, fbo.getTextureID(1));
            GL30.glActiveTexture(GL30.GL_TEXTURE3);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, ssao.getSSAOTexture());
            quadRenderer.renderDeferredLightingPass(ffc.getViewMatrix(), projMatrix, lightPos, ssao.isEnabled());
            OpenGLState.enableDepthTest();
            OpenGLState.enableAlphaBlending();
            pm.render(projMatrix, viewMatrix);
            OpenGLState.disableAlphaBlending();
            OpenGLState.disableDepthTest();
            deferredResult.unbind();
            postProcessPipeline.applyPostProcessing(display, viewMatrix, deferredResult, fbo, lightPos);
            // quadRenderer.renderTextureToScreen(ssao.getSSAOTexture());
            display.flipBuffers();
            display.setFrameTitle("Disguised Phoenix: " + " FPS: " + zeitgeist.getFPS() + ", In frustum objects: " + inViewObjects + ", drawcalls: " + drawCalls + " faces: " + df.format(facesDrawn));
            inViewObjects = 0;
            inViewVerticies = 0;
            facesDrawn = 0;
            drawCalls = 0;
            zeitgeist.sleep();
        }
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

   private static  float noiseScale = 1f / scale;
    private static float change = 0.05f;
    private static void placeEntity(Entity e) {
        Vector3f pos = e.position;
        Random r = new Random();

        float radius = (float) Math.sqrt(Math.pow(scale / 2f, 2) * 3);
        pos.set(r.nextFloat() * 2f - 1f, r.nextFloat() * 2f - 1f, r.nextFloat() * 2f - 1f).normalize(radius);
        pos.mul(1 + SimplexNoise.noise(pos.x * noiseScale, pos.y * noiseScale, pos.z * noiseScale) * change);
        Vector3f eulerAngles = new Vector3f();
        Quaternionf qf = new Quaternionf();
        qf.rotateTo(new Vector3f(0,1,0),new Vector3f(pos).normalize());
        qf.getEulerAnglesXYZ(eulerAngles);
        e.rotX=eulerAngles.x;
        e.rotY=eulerAngles.y;
        e.rotZ=eulerAngles.z;
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
        for (Vector3f vec : sphereVerticies) {
            v[pointer++] = vec.x;
            v[pointer++] = vec.y;
            v[pointer++] = vec.z;
            v[pointer++] = 0;
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
