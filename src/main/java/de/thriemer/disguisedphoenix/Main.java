package de.thriemer.disguisedphoenix;

import de.thriemer.disguisedphoenix.adder.EntityAdder;
import de.thriemer.disguisedphoenix.rendering.MasterRenderer;
import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.disguisedphoenix.terrain.generator.WorldGenerator;
import de.thriemer.engine.input.InputManager;
import de.thriemer.engine.input.KeyboardInputMap;
import de.thriemer.engine.input.MouseInputMap;
import de.thriemer.engine.time.TimerQuery;
import de.thriemer.engine.time.Zeitgeist;
import de.thriemer.engine.util.ModelFileHandler;
import de.thriemer.graphics.camera.Camera;
import de.thriemer.graphics.camera.FreeFlightCamera;
import de.thriemer.graphics.core.context.ContextInformation;
import de.thriemer.graphics.core.context.Display;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.objects.OpenGLState;
import de.thriemer.graphics.core.objects.Vao;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.gui.NuklearBinding;
import de.thriemer.graphics.loader.TextureLoader;
import de.thriemer.graphics.particles.ParticleManager;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL40;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.GL_ONE_MINUS_SRC_ALPHA;

@Slf4j
public class Main {

    public static int inViewVerticies = 0;
    public static int inViewObjects = 0;
    public static int drawCalls = 0;
    public static int facesDrawn = 0;
    public static float radius = 1000;
    //TODO: develop a good distance function to decide when a entity should be renderd and have a shadow
    //get models on itch and cgtrader

    private static float lightAngle = 0;
    private static float lightSpeed = 0.01f;
    private static final float lightRadius = 2 * radius;
    private static final Vector3f lightPos = new Vector3f(0, 1, 0);
    private static final Vector3f lightColor = new Vector3f(1f);

    public static void main(String[] args) {
        //TODO: refactor rendering into a modular pipeline
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 1920, 1080);
        // GL11.glEnable(GL45.GL_DEBUG_OUTPUT);
        MouseInputMap mim = new MouseInputMap();
        ContextInformation contextInformation = display.getContextInformation();
        MasterRenderer masterRenderer = new MasterRenderer(contextInformation);
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
        World world = worldGenerator.generateWorld(pm);
        //world.loadEntireWorld(worldGenerator);
        FreeFlightCamera flightCamera = new FreeFlightCamera(mim, freeFlightCam);
        FreeFlightCamera flightCamera2 = new FreeFlightCamera(mim, freeFlightCam);
        player.movement = kim;
        OpenGLState.enableDepthTest();
        OpenGLState.setAlphaBlending(GL_ONE_MINUS_SRC_ALPHA);
        System.err.println("STARTUP TIME: " + (System.currentTimeMillis() - startUpTime) / 1000f);
        Zeitgeist zeitgeist = new Zeitgeist();
        boolean freeFlightCamActivated = false;
        float time = 1f;
        DecimalFormat df = new DecimalFormat("###,###,###");
        float avgFPS = 0;
        int frameCounter = 0;
        display.setResizeListener(masterRenderer::resize);
        NuklearBinding nuklearBinding = new NuklearBinding(input, display);
        flightCamera.getPosition().set(worldGenerator.getNoiseFunction(new Vector3f(player.position)));
        flightCamera2.getPosition().set((float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f, (float) Math.random() * 2f - 1f);
        float summedMS = 0f;
        // input.hideMouseCursor();
        float switchCameraTimer = 0f;
        float captureMouseTimer = 0f;
        try {
            while (!display.shouldClose() && !input.isKeyDown(GLFW_KEY_ESCAPE)) {
                long startFrame = System.currentTimeMillis();
                float dt = zeitgeist.getDelta();
                if (!input.isKeyDown(GLFW_KEY_H))
                    lightAngle += lightSpeed * dt;
                // lightAngle = 1f;
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
                if (input.isKeyDown(GLFW_KEY_T)) {
                    TimerQuery.resetAll();
                }
                switchCameraTimer -= dt;
                captureMouseTimer -= dt;
                pm.update(dt);
                Camera ffc = flightCamera;
                ffc.update(dt);
                world.updatePlayerPos(ffc.getPosition(), worldGenerator);
                Matrix4f viewMatrix = ffc.getViewMatrix();
                world.update(dt);
                input.updateInputMaps();
                masterRenderer.render(player, ffc, display, viewMatrix, ffc.getPosition(), time, world, lightPos, lightColor);
                screenShot(input,display);
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
        } catch (Exception x) {
            x.printStackTrace();
            System.exit(-1);
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
        Map<Integer, Integer> octreeMap = new HashMap<>();
        world.getStaticEntities().collectStats(0, octreeMap);
        for (Map.Entry e : octreeMap.entrySet()) {
            System.out.println(e.getKey() + ":" + e.getValue());
        }
        worldGenerator.save();
        world.shutdown();
    }

    static long lastScreenshot = 0;

    private static void screenShot(InputManager inputManager, Display display) {
        if (inputManager.isKeyDown(GLFW_KEY_PAUSE) && System.currentTimeMillis() - lastScreenshot > 500) {
            System.out.println("Taking screenshot");
            lastScreenshot = System.currentTimeMillis();
            ContextInformation contextInformation = display.getContextInformation();
            int width = contextInformation.getWidth();
            int height=contextInformation.getHeight();
            BufferedImage image = glScreenshot(width,height);
            try {
                ImageIO.write(image,"PNG", new File("screenshot.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static BufferedImage glScreenshot(int w,int h) {
        GL11.glReadBuffer(GL11.GL_FRONT);
        int bpp = 4;
        ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * bpp);

        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int i = (x + (w * y)) * bpp;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                image.setRGB(x, h - (y + 1), (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }

        return image;
    }

}
