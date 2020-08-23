package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Island;
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
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

public class Main {

    private static int activated = 0;
    private static Map<Model, List<Matrix4f>> modelMap = new HashMap<>();
    private static FrustumIntersection cullingHelper;
    private static Matrix4f projViewMatrix = new Matrix4f();

    public static void main(String args[]) {
        long startUpTime = System.currentTimeMillis();
        Display display = new Display(2560, 1440);
        display.setClearColor(new Color(53 * 2, 81 * 2, 92 * 2));
        // display.setClearColor(new Color(450/3, 450/3, 450/3));
        MouseInputMap mim = new MouseInputMap();
        List<Entity> staticObjects = new ArrayList<>();
        List<Entity> modelActivation = new ArrayList<>();
        cullingHelper = new FrustumIntersection();

        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, 0, 0), mim);
        System.err.println("In the scene are: " + NumberFormat.getIntegerInstance().format(AssimpWrapper.verticies) + " verticies and: " + NumberFormat.getIntegerInstance().format(AssimpWrapper.faces) + " faces!");
        Shader shader = new Shader(Shader.loadShaderCode("testVS.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("pos", "vertexColor");
        shader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "viewMatrix3x3T", "transformationMatrix");
        shader.connectSampler("noiseMap", 0);
        int noiseTexture = TextureLoader.loadTexture("misc/noiseMap.png", GL30.GL_REPEAT, GL30.GL_LINEAR);
        ParticleManager pm = new ParticleManager();
        TestRenderer renderer = new TestRenderer(shader);
        List<Island> flyingIslands = new ArrayList<>();
        Random rnd = new Random();
        for (int i = 0; i < 2; i++) {
            Island land = new Island(generateVec(20000f), rnd.nextFloat() * 10000f + 1000);
            flyingIslands.add(land);
        }
        //  IntStream.range(0, 6).forEach(i -> staticObjects.addAll(generateNextEntities(terrain)));
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective((float) Math.toRadians(70),/*0.8136752f*/16f / 9f, 1f, 100000);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("turnLeft", GLFW.GLFW_KEY_A).addMapping("turnRight", GLFW.GLFW_KEY_D).addMapping("accel", GLFW.GLFW_KEY_SPACE);
        KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("goLeft", GLFW.GLFW_KEY_A).addMapping("goRight", GLFW.GLFW_KEY_D).addMapping("up", GLFW.GLFW_KEY_SPACE).addMapping("down", GLFW.GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight", GLFW.GLFW_KEY_LEFT_CONTROL);
        input.addInputMapping(kim);
        input.addInputMapping(freeFlightCam);
        input.addInputMapping(mim);
        FreeFlightCamera flightCamera = new FreeFlightCamera(mim, freeFlightCam);
        EntityAdder adder = new EntityAdder(pm);
        //adder.getAllEntities(flyingIslands).forEach(e -> addEntity(e, modelMap, staticObjects));
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
        boolean freeFlightCamActivated = false;
        long lastSwitchWireframe = System.currentTimeMillis();
        long lastSwitchCollision = System.currentTimeMillis();
        long lastSwitchCam = System.currentTimeMillis();
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, noiseTexture);
        float time = 0f;
        freeFlightCamActivated = true;
        input.hideMouseCursor();
        while (!display.shouldClose()) {
            float dt = zeitgeist.getDelta();
            time += dt;
            display.pollEvents();
            if (input.isKeyDown(GLFW.GLFW_KEY_O) && System.currentTimeMillis() - lastSwitchWireframe > 100) {
                // wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
                adder.generateNextEntities(player.position, flyingIslands);
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
                player.move(flyingIslands.get(0), dt, staticObjects);
                flightCamera.position.set(player.cam.position);
            } else {
                ffc = flightCamera;
            }
            ffc.update(dt);
            //projViewMatrix.set(new Matrix4f(projMatrix));
            projViewMatrix.identity();
            projViewMatrix.perspective((float) Math.toRadians(50),/*0.8136752f*/16f / 9f, 1f, 100000);
            projViewMatrix.mul(new Matrix4f(ffc.getViewMatrix()));
            cullingHelper.set(projViewMatrix);
            input.updateInputMaps();
            adder.update(dt);
            adder.render(ffc.getViewMatrix(), projMatrix);
            renderer.begin(ffc.getViewMatrix(), projMatrix);
            Matrix3f viewMatrix3x3Transposed = ffc.getViewMatrix().transpose3x3(new Matrix3f());
            shader.loadFloat("time", time);
            shader.loadMatrix("viewMatrix3x3T", viewMatrix3x3Transposed);
            List<Entity> toRemove = new ArrayList<>();
            modelActivation.forEach(entity -> {
                entity.update(dt, modelActivation);
                if (player.position.distance(entity.position) < 60) {
                    toRemove.add(entity);
                    adder.generateNextEntities(player.position, flyingIslands);
                }
                renderer.render(entity.getModel(), entity.getTransformationMatrix());
            });
            modelActivation.removeAll(toRemove);
            adder.getAddedEntities().forEach(e -> addEntity(e, modelMap, staticObjects));
            for (Model m : modelMap.keySet()) {
                float radius = m.radius;
                renderer.render(m, modelMap.get(m).stream().filter(matrix4f -> isInsideFrustum(matrix4f, radius)).toArray(Matrix4f[]::new));
            }
            renderer.render(player.getModel(), player.getTransformationMatrix());
            for (Island terrain : flyingIslands)
                renderer.render(terrain.model, terrain.transformation);
            if (collisionBoxes) {
                display.activateWireframe();

                staticObjects.forEach(entity -> {
                    Collider collider = entity.getCollider();
                    if (collider != null) {
                        collider.allTheShapes.forEach(i -> {
                            if (i instanceof ConvexShape) {
                                ConvexShape cs = (ConvexShape) i;
                                if (cs.canBeRenderd()) {
                                    Vao toRender = cs.getModel();
                                    toRender.bind();
                                    shader.loadMatrix("transformationMatrix", cs.getTransformation());
                                    GL11.glDrawElements(GL11.GL_LINES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                                    toRender.unbind();
                                }
                            }
                        });
                        Vao toRender = collider.boundingBoxModel;
                        toRender.bind();
                        shader.loadMatrix("transformationMatrix", entity.getTransformationMatrix());
                        //    GL11.glDrawElements(GL11.GL_LINES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                        toRender.unbind();
                    }
                });
                Collider playerCollider = player.getCollider();
                playerCollider.allTheShapes.forEach(i -> {
                    if (i instanceof ConvexShape) {
                        ConvexShape cs = (ConvexShape) i;
                        if (cs.canBeRenderd()) {
                            Vao toRender = cs.getModel();
                            toRender.bind();
                            shader.loadMatrix("transformationMatrix", cs.getTransformation());
                            GL11.glDrawElements(GL11.GL_LINES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                            toRender.unbind();
                        }
                    }
                });
                Vao toRender = playerCollider.boundingBoxModel;
                toRender.bind();
                shader.loadMatrix("transformationMatrix", player.getTransformationMatrix());
                GL11.glDrawElements(GL11.GL_LINES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                toRender.unbind();
                display.deactivateWireframe();
            }
            renderer.end();
            pm.render(projMatrix, ffc.getViewMatrix());
            display.flipBuffers();
            display.setFrameTitle("Disguised Phoenix: " + zeitgeist.getFPS() + " FPS");
            zeitgeist.sleep();
        }
        TextureLoader.cleanUpAllTextures();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

    private static void addEntity(Entity entity, Map<Model, List<Matrix4f>> modelMap, List<Entity> staticEntities) {
        Model m = entity.getModel();
        modelMap.computeIfAbsent(m, k -> new ArrayList<>());
        modelMap.get(m).add(entity.getTransformationMatrix());
        staticEntities.add(entity);
    }

    private static Entity generateActivationSwitch(Island terrain) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * terrain.getSize() + terrain.position.x;
        float z = rnd.nextFloat() * terrain.getSize() + terrain.position.z;
        float h = terrain.getHeightOfTerrain(x, terrain.position.y, z);
        return new RotatingEntity(ModelFileHandler.getModel("cube.modelFile"), x, h + 50, z, 40);
    }

    private static Vector3f generateVec(float bounds) {
        return new Vector3f((float) (Math.random() * 2f - 1f) * bounds, (float) (Math.random() * 2f - 1f) * bounds, (float) (Math.random() * 2f - 1f) * bounds);
    }

    private static Vector3f tempVec = new Vector3f();

    private static boolean isInsideFrustum(Matrix4f matrix, float radius) {
       float scale = matrix.getScale(tempVec).x;
        Vector3f translation = matrix.getTranslation(tempVec);
        return cullingHelper.testSphere(translation, radius * scale);
    }

}
