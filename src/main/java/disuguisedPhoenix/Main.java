package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Island;
import disuguisedPhoenix.terrain.World;
import engine.collision.Collider;
import engine.collision.ConvexShape;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.ModelFileHandler;
import engine.util.Zeitgeist;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.context.Display;
import graphics.loader.AssimpWrapper;
import graphics.loader.TextureLoader;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.particles.ParticleManager;
import graphics.renderer.TestRenderer;
import graphics.world.Model;
import org.joml.FrustumIntersection;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

public class Main {

    private static Matrix4f projViewMatrix = new Matrix4f();

    public static int drawCalls=0;
    public static int facesDrawn=0;

    public static void main(String[] args) {
        long startUpTime = System.currentTimeMillis();
        Display display = new Display(2560, 1440);
        display.setClearColor(new Color(59, 168, 198));
        // display.setClearColor(new Color(450/3, 450/3, 450/3));
        MouseInputMap mim = new MouseInputMap();
        ParticleManager pm = new ParticleManager();

        World world = new World(pm);
        for(int i=0;i<50;i++)world.addIsland(100000);
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, 0, 0), mim);
        Shader shader = new Shader(Shader.loadShaderCode("testVS.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("pos", "vertexColor");
        shader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "viewMatrix3x3T", "transformationMatrix");
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
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glCullFace(GL12.GL_BACK);
        GL11.glEnable(GL11.GL_CULL_FACE);
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
                 wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
               // adder.generateNextEntities(player.position, flyingIslands);
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
                display.activateWireframe();
            } else {
                display.deactivateWireframe();
            }
            display.clear();
            pm.update(dt);
            Camera ffc = player.cam;
            if (!freeFlightCamActivated) {
                player.move(world.islands.get(0).island, dt, world.getPossibleCollisions(player));
                flightCamera.position.set(player.cam.position);
            } else {
                ffc = flightCamera;
            }
            ffc.update(dt);
            input.updateInputMaps();
            Matrix4f viewMatrix = ffc.getViewMatrix();
            renderer.begin(viewMatrix, projMatrix);
            Matrix3f viewMatrix3x3Transposed = ffc.getViewMatrix().transpose3x3(new Matrix3f());
            shader.loadFloat("time", time);
            shader.loadMatrix("viewMatrix3x3T", viewMatrix3x3Transposed);
            world.render(renderer,projMatrix,viewMatrix,ffc.position);
            renderer.render(player.getModel(), player.getTransformationMatrix());
            pm.render(projMatrix, ffc.getViewMatrix());
            display.setFrameTitle("Disguised Phoenix: " + zeitgeist.getFPS() + " FPS "+" "+drawCalls+" draw calls "+df.format(facesDrawn)+" faces" );
            display.flipBuffers();
            drawCalls=0;
            facesDrawn=0;
            zeitgeist.sleep();
        }
        TextureLoader.cleanUpAllTextures();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

    private static void addEntity(Entity entity, Map<Model, List<Entity>> modelMap, List<Entity> staticEntities) {
        Model m = entity.getModel();
        modelMap.computeIfAbsent(m, k -> new ArrayList<>());
        modelMap.get(m).add(entity);
        staticEntities.add(entity);
    }

    private static Entity generateActivationSwitch(Island terrain) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * terrain.getSize() + terrain.position.x;
        float z = rnd.nextFloat() * terrain.getSize() + terrain.position.z;
        float h = terrain.getHeightOfTerrain(x, terrain.position.y, z);
        return new RotatingEntity(ModelFileHandler.getModel("cube.modelFile"), x, h + 50, z, 40);
    }

}
