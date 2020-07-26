package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Terrain;
import engine.collision.Collider;
import engine.collision.ConvexShape;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.Zeitgeist;
import graphics.camera.Camera;
import graphics.camera.FreeFlightCamera;
import graphics.context.Display;
import graphics.loader.ModelLoader;
import graphics.loader.TextureLoader;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.particles.ParticleManager;
import graphics.renderer.TestRenderer;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static int activated = 0;
    private static Map<Model, List<Matrix4f>> modelMap = new HashMap<>();


    public static void main(String args[]) {
        long startUpTime = System.currentTimeMillis();
        Display display = new Display(1920, 1280);
        display.setClearColor(new Color(53*2,81*2,92*2));
        MouseInputMap mim = new MouseInputMap();
        List<Entity> staticObjects = new ArrayList<>();
        List<Entity> modelActivation = new ArrayList<>();

        Player player = new Player(ModelLoader.getModel("misc/quickBirb.obj", "pfalz/collider.obj"), new Vector3f(-40, 0, -40), mim);
        Model propellor = ModelLoader.getModel("pfalz/propellor.obj");
        System.err.println("In the scene are: " + NumberFormat.getIntegerInstance().format(ModelLoader.verticies) + " verticies and: " + NumberFormat.getIntegerInstance().format(ModelLoader.faces) + " faces!");
        Shader shader = new Shader(Shader.loadShaderCode("testVS.glsl"), Shader.loadShaderCode("testFS.glsl")).combine();
        shader.bindAtrributs("pos", "normals").loadUniforms("projMatrix", "viewMatrix", "transformationMatrix", "diffuse", "usesTexture", "diffuseTexture", "ambient", "specular", "normalTexture", "usesNormalTexture", "shininess", "opacity");
        shader.connectSampler("diffuseTexture", 0);
        shader.connectSampler("normalTexture", 1);
        ParticleManager pm = new ParticleManager();
        TestRenderer renderer = new TestRenderer(shader);

        Terrain terrain = new Terrain(new Vector3f(0, 0, 0));
        //  IntStream.range(0, 6).forEach(i -> modelActivation.add(generateActivationSwitch(terrain)));
      //  IntStream.range(0, 6).forEach(i -> staticObjects.addAll(generateNextEntities(terrain)));
        Matrix4f terrainTransformation = new Matrix4f();
        terrainTransformation.translate(0, 0, 0);
        Matrix4f projMatrix = new Matrix4f();
        projMatrix.perspective((float) Math.toRadians(70), /*0.8136752f*/16f / 9f, 1f, 100000);
        InputManager input = new InputManager(display.getWindowId());
        KeyboardInputMap kim = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("turnLeft", GLFW.GLFW_KEY_A).addMapping("turnRight", GLFW.GLFW_KEY_D).addMapping("accel", GLFW.GLFW_KEY_SPACE);
        KeyboardInputMap freeFlightCam = new KeyboardInputMap().addMapping("forward", GLFW.GLFW_KEY_W).addMapping("backward", GLFW.GLFW_KEY_S).addMapping("goLeft", GLFW.GLFW_KEY_A).addMapping("goRight", GLFW.GLFW_KEY_D).addMapping("up", GLFW.GLFW_KEY_SPACE).addMapping("down", GLFW.GLFW_KEY_LEFT_SHIFT).addMapping("fastFlight", GLFW.GLFW_KEY_LEFT_CONTROL);
        input.addInputMapping(kim);
        input.addInputMapping(freeFlightCam);
        input.addInputMapping(mim);
        //FreeFlightCamera ffc = new FreeFlightCamera(mim, freeFlightCam);
        EntityAdder adder = new EntityAdder(pm);
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
        long lastSwitchWireframe = System.currentTimeMillis();
        long lastSwitchCollision = System.currentTimeMillis();
       input.hideMouseCursor();
        while (!display.shouldClose()) {
            //player.setPosition(new Vector3f(0));
            float dt = zeitgeist.getDelta();
            display.pollEvents();
            if (input.isKeyDown(GLFW.GLFW_KEY_O) && System.currentTimeMillis() - lastSwitchWireframe > 100) {
                wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
                adder.generateNextEntities(player.position,terrain);
            }
            if (input.isKeyDown(GLFW.GLFW_KEY_L) && System.currentTimeMillis() - lastSwitchCollision > 100) {
                collisionBoxes = !collisionBoxes;
                lastSwitchCollision = System.currentTimeMillis();
            }
            if (wireframe) {
               // display.activateWireframe();
            }
            display.clear();
            pm.update(dt);
            player.move(terrain, dt, staticObjects);
           Camera ffc = player.cam;
            ffc.update(dt);
            input.updateInputMaps();
            adder.update(dt);
            adder.render(ffc.getViewMatrix(),projMatrix);
            renderer.begin(ffc.getViewMatrix(), projMatrix);
            List<Entity> toRemove = new ArrayList<>();
            modelActivation.forEach(entity -> {
                entity.update(dt, modelActivation);
                if (player.position.distance(entity.position) < 60) {
                    toRemove.add(entity);
                    adder.generateNextEntities(player.position, terrain);
                }
                renderer.render(entity.getModel(), entity.getTransformationMatrix());
            });
            modelActivation.removeAll(toRemove);
            adder.getAddedEntities().forEach(e->addEntity(e,modelMap,staticObjects));
            for (Model m : modelMap.keySet()) {
                renderer.render(m, modelMap.get(m).stream().toArray(Matrix4f[]::new));
            }
            renderer.render(player.getModel(), player.getTransformationMatrix());
            renderer.render(propellor, player.getPropellorMatrix());
            renderer.render(terrain.model, terrainTransformation);
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
                playerCollider.allTheShapes.forEach(i -> {
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
            }
            renderer.end();
            pm.render(projMatrix,ffc.getViewMatrix());
            display.flipBuffers();
            display.setFrameTitle("Disguised Phoenix: " + zeitgeist.getFPS() + " FPS");
            zeitgeist.sleep();
        }
        TextureLoader.cleanUpAllTextures();
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

    private static void addEntity(Entity entity,Map<Model,List<Matrix4f>> modelMap,List<Entity> staticEntities){
        Model m = entity.getModel();
        if (modelMap.get(m) == null) {
            modelMap.put(m, new ArrayList<>());
        }
        modelMap.get(m).add(entity.getTransformationMatrix());
    staticEntities.add(entity);
    }

    private static Entity generateActivationSwitch(Terrain terrain) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * Terrain.SIZE;
        float z = rnd.nextFloat() * Terrain.SIZE;
        float h = terrain.getHeightOfTerrain(x, z);
        return new RotatingEntity(ModelLoader.getModel("cube.obj"), x, h + 50, z, 40);
    }
}
