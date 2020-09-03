package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Island;
import disuguisedPhoenix.terrain.World;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.ModelFileHandler;
import engine.util.Zeitgeist;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.context.Display;
import graphics.loader.TextureLoader;
import graphics.objects.OpenGLState;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.particles.ParticleManager;
import graphics.renderer.MultiIndirectRenderer;
import graphics.renderer.TestRenderer;
import graphics.world.Model;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main {

    public static int drawCalls = 0;
    public static int facesDrawn = 0;

    public static void main(String[] args) {
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 640, 480);
        display.setClearColor(new Color(59, 168, 198));
        // display.setClearColor(new Color(450/3, 450/3, 450/3));
        MouseInputMap mim = new MouseInputMap();
        ParticleManager pm = new ParticleManager();
        MultiIndirectRenderer multiRenderer = new MultiIndirectRenderer();
        ModelFileHandler.loadModelsForMultiDraw(multiRenderer.matrixVbo, "plants/flowerTest1.modelFile", "lowPolyTree/vc.modelFile", "lowPolyTree/ballTree.modelFile", "lowPolyTree/bendyTree.modelFile", "lowPolyTree/tree2.modelFile", "misc/rock.modelFile", "plants/grass.modelFile", "plants/mushroom.modelFile", "misc/tutorialCrystal.modelFile", "plants/glockenblume.modelFile");
        World world = new World(pm);
        for (int i = 0; i < 50; i++) world.addIsland(20000);
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, 0, 0), mim);
        Shader shader = new Shader(Shader.loadShaderCode("testVS.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("pos", "vertexColor");
        shader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "viewMatrix3x3T", "transformationMatrix");
        shader.connectSampler("noiseMap", 0);
        Shader multiDrawShader = new Shader(Shader.loadShaderCode("testVSMultiDraw.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("pos", "vertexColor","transformationMatrix");
        multiDrawShader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "viewMatrix3x3T");
        multiDrawShader.connectSampler("noiseMap", 0);
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
        OpenGLState.enableBackFaceCulling();
        System.err.println("STARTUP TIME: " + (System.currentTimeMillis() - startUpTime) / 1000f);
        //  display.activateWireframe();
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
        while (!display.shouldClose()) {
            float dt = zeitgeist.getDelta();
            time += dt;
            display.pollEvents();
            if (input.isKeyDown(GLFW.GLFW_KEY_O) && System.currentTimeMillis() - lastSwitchWireframe > 100) {
           //     wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
                world.addNextEntities(player.position);
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
            display.clear();
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
            renderer.begin(viewMatrix, projMatrix);
            Matrix3f viewMatrix3x3Transposed = ffc.getViewMatrix().transpose3x3(new Matrix3f());
            shader.loadFloat("time", time);
            shader.loadMatrix("viewMatrix3x3T", viewMatrix3x3Transposed);
            renderer.render(player.getModel(), player.getTransformationMatrix());
            world.render(renderer, projMatrix, viewMatrix, ffc.position);
            multiDrawShader.bind();
            multiDrawShader.loadFloat("time", time);
            multiDrawShader.loadMatrix("viewMatrix3x3T", viewMatrix3x3Transposed);
            multiDrawShader.loadMatrix("projMatrix", projMatrix);
            multiDrawShader.loadMatrix("viewMatrix", viewMatrix);
            multiRenderer.render(world.getVisibleEntities(projMatrix,viewMatrix,ffc.position));
            multiDrawShader.unbind();
            pm.render(projMatrix, ffc.getViewMatrix());
            display.setFrameTitle("Disguised Phoenix: " + zeitgeist.getFPS() + " FPS " + " " + drawCalls + " draw calls " + df.format(facesDrawn) + " faces");
            display.flipBuffers();
            drawCalls = 0;
            facesDrawn = 0;
            zeitgeist.sleep();
        }
        TextureLoader.cleanUpAllTextures();
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

}
