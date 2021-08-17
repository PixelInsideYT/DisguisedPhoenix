package disuguisedphoenix;

import disuguisedphoenix.adder.EntityAdder;
import disuguisedphoenix.rendering.MasterRenderer;
import disuguisedphoenix.terrain.World;
import engine.collision.CollisionShape;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.ModelFileHandler;
import engine.util.Zeitgeist;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.core.context.Display;
import graphics.core.objects.FrameBufferObject;
import graphics.core.objects.OpenGLState;
import graphics.core.objects.Vao;
import graphics.core.renderer.MultiIndirectRenderer;
import graphics.core.shaders.ComputeShader;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;
import graphics.gui.NuklearBinding;
import graphics.loader.TextureLoader;
import graphics.modelinfo.Model;
import graphics.modelinfo.RenderInfo;
import graphics.particles.ParticleManager;
import org.joml.Matrix4f;
import org.joml.SimplexNoise;
import org.joml.Vector3f;
import org.lwjgl.opengl.GLUtil;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;

public class Main {

    public static int inViewVerticies = 0;
    public static int inViewObjects = 0;
    public static int drawCalls = 0;
    public static int facesDrawn = 0;
    public static float radius = 500;
    private static Model model;

    //get models on itch and cgtrader
    private static List<CollisionShape> worldTris = new ArrayList<>();

    private static float lightAngle = 0;
    private static float lightRadius = 2 * radius;
    private static Vector3f lightPos = new Vector3f(0, 1, 0);
    private static Vector3f lightColor = new Vector3f(1f);
    private static float noiseScale = 0.01f;
    private static float change = 0.1f;

    public static void main(String[] args) {
        //TODO: refactor rendering into a modular pipeline
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 1920, 1080);
        // GL11.glEnable(GL45.GL_DEBUG_OUTPUT);
        //GLUtil.setupDebugMessageCallback();
        model = createSphere();
        MouseInputMap mim = new MouseInputMap();
        ParticleManager pm = new ParticleManager();
        int width = display.getWidth();
        int height = display.getHeight();
        float aspectRatio = width / (float) height;
        MasterRenderer masterRenderer = new MasterRenderer(width,height,aspectRatio);
        ModelFileHandler.loadModelsForMultiDraw(masterRenderer.multiIndirectRenderer.persistantMatrixVbo, EntityAdder.getModelNameList().stream().toArray(String[]::new));
        World world = new World(pm, 4f * radius);
        EntityAdder ea = world.getEntityAdder();
        ea.getAllEntities(4f * 3.141592f * radius * radius, Main::getPositionOnEarthFromDirection).forEach(e -> world.placeUprightEntity(e, e.position));
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, radius, 0), mim);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW_KEY_W).addMapping("backward", GLFW_KEY_S).addMapping("turnLeft", GLFW_KEY_A).addMapping("turnRight", GLFW_KEY_D).addMapping("accel", GLFW_KEY_SPACE);
        KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW_KEY_W).addMapping("backward", GLFW_KEY_S).addMapping("goLeft", GLFW_KEY_A).addMapping("goRight", GLFW_KEY_D).addMapping("up", GLFW_KEY_SPACE).addMapping("down", GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight", GLFW_KEY_LEFT_CONTROL);
        input.addInputMapping(kim);
        input.addInputMapping(freeFlightCam);
        input.addInputMapping(mim);
        FreeFlightCamera flightCamera = new FreeFlightCamera(mim, freeFlightCam);
        FreeFlightCamera flightCamera2 = new FreeFlightCamera(mim, freeFlightCam);
        player.movement = kim;
        OpenGLState.enableDepthTest();
        OpenGLState.setAlphaBlending(GL_ONE_MINUS_SRC_ALPHA);
        System.err.println("STARTUP TIME: " + (System.currentTimeMillis() - startUpTime) / 1000f);
        Zeitgeist zeitgeist = new Zeitgeist();
        boolean wireframe = false;
        boolean freeFlightCamActivated = false;
        float time = 1f;
        DecimalFormat df = new DecimalFormat("###,###,###");
        float avgFPS = 0;
        int frameCounter = 0;
        display.setResizeListener(masterRenderer::resize);
        NuklearBinding nuklearBinding = new NuklearBinding(input, display);
        flightCamera.getPosition().set(player.position);
        flightCamera2.getPosition().set((float)Math.random()*2f-1f,(float)Math.random()*2f-1f,(float)Math.random()*2f-1f);
        flightCamera2.getPosition().set(Main.getPositionOnEarthFromDirection(flightCamera2.getPosition()));
        float summedMS = 0f;
       // input.hideMouseCursor();
        float switchCameraTimer = 0f;
        float captureMouseTimer = 0f;
/*
        int tex_output = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tex_output);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, 1920, 1080, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);

        ComputeShader computeShader = new ComputeShader(ShaderFactory.loadShaderCode("compute/hizTest.glsl"));
        computeShader.loadUniform("hiZ");
*/
        while (!display.shouldClose() && !input.isKeyDown(GLFW_KEY_ESCAPE)) {
            long startFrame = System.currentTimeMillis();
            float dt = zeitgeist.getDelta();
            // lightAngle += lightSpeed * dt;
            lightAngle = 1f;
            lightPos.z = (float) Math.cos(lightAngle) * lightRadius;
            lightPos.y = (float) Math.sin(lightAngle) * lightRadius;
            time += dt;
            display.pollEvents();
            // nuklearBinding.pollInputs();
            if (input.isKeyDown(GLFW_KEY_P)&&captureMouseTimer<0) {
                input.toggleCursor();
                captureMouseTimer=0.25f;
            }
            if (input.isKeyDown(GLFW_KEY_C) && switchCameraTimer < 0) {
                freeFlightCamActivated = !freeFlightCamActivated;
                switchCameraTimer = 0.25f;
            }
            switchCameraTimer -= dt;
            captureMouseTimer-=dt;
            if (wireframe) {
                OpenGLState.enableWireframe();
            } else {
                OpenGLState.disableWireframe();
            }
            pm.update(dt);
            Camera ffc;
            if (!freeFlightCamActivated) {
                ffc = flightCamera2;
            } else {
                ffc = flightCamera;
            }
            ffc.update(dt);
            Matrix4f viewMatrix = ffc.getViewMatrix();
            world.update(dt);
            input.updateInputMaps();
            masterRenderer.render(display, viewMatrix,time,world,ffc.getPosition(),lightPos,lightColor,model);
            display.clear();
            avgFPS += zeitgeist.getFPS();
            display.setFrameTitle("Disguised Phoenix: " + " FPS: " + zeitgeist.getFPS() + ", In frustum objects: " + inViewObjects + ", drawcalls: " + drawCalls + " faces: " + df.format(facesDrawn));
            inViewObjects = 0;
            inViewVerticies = 0;
            facesDrawn = 0;
            drawCalls = 0;
            zeitgeist.sleep();
            frameCounter++;
            summedMS += (System.currentTimeMillis() - startFrame);
        }
        masterRenderer.print();
        nuklearBinding.cleanUp();
        System.out.println("AVG FPS: " + (avgFPS / (float) frameCounter));
        System.out.println("AVG MS: " + (summedMS / (float) frameCounter));
        TextureLoader.cleanUpAllTextures();
        FrameBufferObject.cleanUpAllFbos();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

    public static Vector3f getPositionOnEarthFromDirection(Vector3f v) {
        return v.normalize(radius).mul(1 + SimplexNoise.noise(v.x * noiseScale, v.y * noiseScale, v.z * noiseScale) * change);
    }

    private static Model createSphere() {
        float scale = 100;
        int subdivisions = 100;
        List<Vector3f> sphereVerticies = createPlane(subdivisions, scale, 1, 0, 0);
        int indicyOffset = sphereVerticies.size();
        sphereVerticies.addAll(createPlane(subdivisions, scale, -1, 0, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, -1, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 1, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 0, 1));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 0, -1));

        sphereVerticies.forEach(Main::getPositionOnEarthFromDirection);
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
