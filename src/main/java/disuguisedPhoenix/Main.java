package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Terrain;
import engine.collision.Collider;
import engine.collision.ConvexShape;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import graphics.camera.Camera;
import graphics.context.Display;
import graphics.loader.ModelLoader;
import graphics.loader.TextureLoader;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.renderer.TestRenderer;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static int activated = 0;

    public static void main(String args[]) {
        long startUpTime = System.currentTimeMillis();
        Display display = new Display(1080, 720);
        MouseInputMap mim = new MouseInputMap();
        List<Entity> staticObjects = new ArrayList<>();
        List<Entity> modelActivation = new ArrayList<>();

        Player player = new Player(ModelLoader.getModel("pfalz/flugzeug.obj","pfalz/collider.obj"), new Vector3f(-40, 0, -40), mim);
        Model propellor = ModelLoader.getModel("pfalz/propellor.obj");
        System.err.println("In the scene are: " + NumberFormat.getIntegerInstance().format(ModelLoader.verticies) + " verticies and: " + NumberFormat.getIntegerInstance().format(ModelLoader.faces) + " faces!");
        Shader shader = new Shader(Shader.loadShaderCode("testVS"), Shader.loadShaderCode("testFS")).combine();
        shader.bindAtrributs("pos", "textureCoords", "normals").loadUniforms("projMatrix", "viewMatrix", "transformationMatrix", "diffuse", "usesTexture", "diffuseTexture", "ambient", "specular", "normalTexture", "usesNormalTexture", "shininess", "opacity");
        shader.connectSampler("diffuseTexture", 0);
        shader.connectSampler("normalTexture", 1);
        TestRenderer renderer = new TestRenderer(shader);
        Terrain terrain = new Terrain(new Vector3f(0, 0, 0));
        //  IntStream.range(0, 5).forEach(i -> staticObjects.addAll(generateNextEntities(terrain))/*  modelActivation.add(generateActivationSwitch(terrain))*/);
        staticObjects.addAll(IntStream.range(0, 10).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/tree2.obj", 0, 6f, 0, 30)).collect(Collectors.toList()));

        Matrix4f terrainTransformation = new Matrix4f();
        terrainTransformation.translate(0, 0, 0);
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective((float) Math.toRadians(70), /*0.8136752f*/16f / 9f, 1f, 100000);
        display.setClearColor(Color.white);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("turnLeft", GLFW.GLFW_KEY_A).addMapping("turnRight", GLFW.GLFW_KEY_D).addMapping("accel", GLFW.GLFW_KEY_SPACE);
        // KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("goLeft", GLFW.GLFW_KEY_A).addMapping("goRight", GLFW.GLFW_KEY_D).addMapping("up", GLFW.GLFW_KEY_SPACE).addMapping("down", GLFW.GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight",GLFW.GLFW_KEY_LEFT_CONTROL);
        Model mesh = ModelLoader.getModel("lowPolyTree/tree2Collider.obj");
        input.addInputMapping(kim);
        input.addInputMapping(mim);
        // FreeFlightCamera ffc = new FreeFlightCamera(mim, freeFlightCam);
        player.movement = kim;
        Model cube = ModelLoader.getModel("cube.obj");
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glCullFace(GL12.GL_BACK);
        // GL11.glEnable(GL11.GL_CULL_FACE);
        // input.hideMouseCursor();
        //  input.addInputMapping(freeFlightCam);
        System.err.println("STARTUP TIME: " + (System.currentTimeMillis() - startUpTime) / 1000f);
        //  display.activateWireframe();
        int minFps = Integer.MAX_VALUE;
        int maxFPS = Integer.MIN_VALUE;
        float avgFps = 0;
        float count = 0;
        while (!display.shouldClose()) {
            long time = System.currentTimeMillis();
            display.pollEvents();
            display.clear();
            Camera ffc = player.cam;
            ffc.update(1f / 60f);
            input.updateInputMaps();
            player.move(terrain, 1f / 60f, staticObjects);
            renderer.begin(ffc.getViewMatrix(), projMatrix);
           /* List<Entity> toRemove = new ArrayList<>();
            modelActivation.forEach(entity -> {
                entity.update(1f / 60f);
                if (player.position.distance(entity.position) < 60) {
                    toRemove.add(entity);
                    staticObjects.addAll(generateNextEntities(terrain));
                }
                renderer.render(entity.getModel(), entity.getTransformationMatrix());
            });
            modelActivation.removeAll(toRemove);*/
            staticObjects.forEach(entity -> renderer.render(entity.getModel(), entity.getTransformationMatrix()));
            renderer.render(player.getModel(), player.getTransformationMatrix());
            renderer.render(propellor, player.getPropellorMatrix());
            renderer.render(terrain.model, terrainTransformation);
            display.activateWireframe();

            staticObjects.forEach(entity -> {
                Collider collider = entity.getCollider();
                if (collider != null) {
                    collider.allTheShapes.stream().forEach(i -> {
                        if (i instanceof ConvexShape) {
                            ConvexShape cs = (ConvexShape) i;
                            if (cs.canBeRenderd()) {
                                Vao toRender = cs.getModel();
                                toRender.bind();
                                shader.loadMatrix("transformationMatrix", cs.getTransformation());
                                GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                                toRender.unbind();
                            }
                        }
                    });
                    Vao toRender = collider.boundingBoxModel;
                    toRender.bind();
                    shader.loadMatrix("transformationMatrix", entity.getTransformationMatrix());
                    GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                    toRender.unbind();
                }
            });
            Collider playerCollider = player.getCollider();
           playerCollider.allTheShapes.stream().forEach(i -> {
                if (i instanceof ConvexShape) {
                    ConvexShape cs = (ConvexShape) i;
                    if (cs.canBeRenderd()) {
                        Vao toRender = cs.getModel();
                        toRender.bind();
                        shader.loadMatrix("transformationMatrix", cs.getTransformation());
                        GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
                        toRender.unbind();
                    }
                }
            });
            Vao toRender = playerCollider.boundingBoxModel;
            toRender.bind();
            shader.loadMatrix("transformationMatrix", player.getTransformationMatrix());
            GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
            toRender.unbind();
            display.deactivateWireframe();
            renderer.end();
            display.flipBuffers();
            float fps = 1f / ((System.currentTimeMillis() - time) / 1000f);
            minFps = Math.min(minFps, (int) fps);
            maxFPS = Math.max(maxFPS, (int) fps);
            avgFps += fps;
            count++;
            display.setFrameTitle("Disguised Phoenix: " + (int) fps);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
        System.out.println("min: " + minFps + " max: " + maxFPS + " avg: " + avgFps / count);
        TextureLoader.cleanUpAllTextures();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

    private static List<Entity> generateNextEntities(Terrain terrain) {
        switch (activated) {
            case 0:
                activated++;
                return IntStream.range(0, 10000).mapToObj(i -> generateEntiy(terrain, "plants/grass.obj", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 1:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/testTree.obj", 0, 6f, 0, 40)).collect(Collectors.toList());
            case 2:
                activated++;
                return IntStream.range(0, 250).mapToObj(i -> generateEntiy(terrain, "plants/flowerTest1.obj", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 3:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/tree2.obj", 0, 6f, 0, 30)).collect(Collectors.toList());
            case 4:
                activated++;
                return IntStream.range(0, 500).mapToObj(i -> generateEntiy(terrain, "misc/rock.obj", 6f, 6f, 6f, 10)).collect(Collectors.toList());

        }
        return null;
    }

    private static Entity generateEntiy(Terrain terrain, String modelName, float rotRandomX, float rotRandomY, float rotRandomZ, float scale) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * Terrain.SIZE;
        float z = rnd.nextFloat() * Terrain.SIZE;
        float h = terrain.getHeightOfTerrain(x, z);
        float scaleDiffrence = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        Entity e = new Entity(ModelLoader.getModel(modelName,"lowPolyTree/tree2Collider.obj"), new Vector3f(x, h, z), rnd.nextFloat() * rotRandomX, rnd.nextFloat() * rotRandomY, rnd.nextFloat() * rotRandomZ, scale * scaleDiffrence);
        return e;
    }

    private static Entity generateActivationSwitch(Terrain terrain) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * Terrain.SIZE;
        float z = rnd.nextFloat() * Terrain.SIZE;
        float h = terrain.getHeightOfTerrain(x, z);
        RotatingEntity re = new RotatingEntity(ModelLoader.getModel("cube.obj"), x, h + 50, z, 40);
        return re;
    }
}
