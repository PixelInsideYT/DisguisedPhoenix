package disuguisedphoenix;

import disuguisedphoenix.adder.EntityAdder;
import disuguisedphoenix.rendering.MasterRenderer;
import disuguisedphoenix.terrain.World;
import disuguisedphoenix.terrain.generator.WorldGenerator;
import engine.collision.CollisionShape;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.time.TimerQuery;
import engine.time.Zeitgeist;
import engine.util.ModelFileHandler;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.core.context.Display;
import graphics.core.objects.FrameBufferObject;
import graphics.core.objects.OpenGLState;
import graphics.core.objects.Vao;
import graphics.core.shaders.Shader;
import graphics.gui.NuklearBinding;
import graphics.loader.TextureLoader;
import graphics.modelinfo.Model;
import graphics.modelinfo.RenderInfo;
import graphics.particles.ParticleManager;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.SimplexNoise;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.GL_ONE_MINUS_SRC_ALPHA;

@Slf4j
public class Main {

    public static int inViewVerticies = 0;
    public static int inViewObjects = 0;
    public static int drawCalls = 0;
    public static int facesDrawn = 0;
    public static float radius = 500;
    private static Model model;

    //get models on itch and cgtrader

    private static float lightAngle = 0;
    private static final float lightRadius = 2 * radius;
    private static final Vector3f lightPos = new Vector3f(0, 1, 0);
    private static final Vector3f lightColor = new Vector3f(1f);

    public static void main(String[] args) {
        //TODO: refactor rendering into a modular pipeline
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 1920, 1080);
        // GL11.glEnable(GL45.GL_DEBUG_OUTPUT);3
        MouseInputMap mim = new MouseInputMap();
        int width = display.getWidth();
        int height = display.getHeight();
        float aspectRatio = width / (float) height;
        MasterRenderer masterRenderer = new MasterRenderer(width, height, aspectRatio);
        ModelFileHandler.loadModelsForMultiDraw(masterRenderer.getMultiDrawVBO(), EntityAdder.getModelNameList().toArray(new String[0]));
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, radius, 0), mim);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW_KEY_W).addMapping("backward", GLFW_KEY_S).addMapping("turnLeft", GLFW_KEY_A).addMapping("turnRight", GLFW_KEY_D).addMapping("accel", GLFW_KEY_SPACE);
        KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW_KEY_W).addMapping("backward", GLFW_KEY_S).addMapping("goLeft", GLFW_KEY_A).addMapping("goRight", GLFW_KEY_D).addMapping("up", GLFW_KEY_SPACE).addMapping("down", GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight", GLFW_KEY_LEFT_CONTROL);
        input.addInputMapping(kim);
        input.addInputMapping(freeFlightCam);
        input.addInputMapping(mim);
        //GLUtil.setupDebugMessageCallback();
        ParticleManager pm = new ParticleManager();
        WorldGenerator worldGenerator = new WorldGenerator(radius);
        model = worldGenerator.generateSphereSurface();
        World world = worldGenerator.generateWorld(pm);
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
        flightCamera2.getPosition().set((float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f);
        float summedMS = 0f;
        // input.hideMouseCursor();
        float switchCameraTimer = 0f;
        float captureMouseTimer = 0f;
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
            if (input.isKeyDown(GLFW_KEY_P) && captureMouseTimer < 0) {
                input.toggleCursor();
                captureMouseTimer = 0.25f;
            }
            if (input.isKeyDown(GLFW_KEY_C) && switchCameraTimer < 0) {
                freeFlightCamActivated = !freeFlightCamActivated;
                switchCameraTimer = 0.25f;
            }
            switchCameraTimer -= dt;
            captureMouseTimer -= dt;
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
            masterRenderer.render(display, viewMatrix, ffc.getPosition(), time, world, lightPos, lightColor, model);
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
        TimerQuery.printAllResults();
        nuklearBinding.cleanUp();
        log.info("AVG FPS: {}", avgFPS / frameCounter);
        log.info("AVG MS: {}", summedMS / frameCounter);
        TextureLoader.cleanUpAllTextures();
        FrameBufferObject.cleanUpAllFbos();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

}
