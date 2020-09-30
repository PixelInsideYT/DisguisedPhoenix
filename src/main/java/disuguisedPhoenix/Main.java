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
import graphics.objects.FrameBufferObject;
import graphics.objects.OpenGLState;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.particles.ParticleManager;
import graphics.postProcessing.*;
import graphics.postProcessing.SSAO.SSAOEffect;
import graphics.renderer.MultiIndirectRenderer;
import graphics.renderer.TestRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Random;

public class Main {

    public static int drawCalls = 0;
    public static int facesDrawn = 0;

    public static void main(String[] args) {
        long startUpTime = System.currentTimeMillis();
        Display display = new Display("Disguised Phoenix", 1920, 1080);
        display.setClearColor(new Color(59, 168, 198));
        // GLUtil.setupDebugMessageCallback();
        // display.setClearColor(new Color(450/3, 450/3, 450/3));
        MouseInputMap mim = new MouseInputMap();
        ParticleManager pm = new ParticleManager();
        MultiIndirectRenderer multiRenderer = new MultiIndirectRenderer();
        ModelFileHandler.loadModelsForMultiDraw(multiRenderer.persistantMatrixVbo, "plants/flowerTest1.modelFile", "lowPolyTree/vc.modelFile", "lowPolyTree/ballTree.modelFile", "lowPolyTree/bendyTree.modelFile", "lowPolyTree/tree2.modelFile", "misc/rock.modelFile", "plants/grass.modelFile", "plants/mushroom.modelFile", "misc/tutorialCrystal.modelFile", "plants/glockenblume.modelFile", "misc/fox.modelFile");
        World world = new World(pm);
        for (int i = 0; i < 50; i++) world.addIsland(20000);
        Player player = new Player(ModelFileHandler.getModel("misc/birb.modelFile"), new Vector3f(0, 0, 0), mim);
        Shader shader = new Shader(Shader.loadShaderCode("testVS.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("pos", "vertexColor");
        shader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "transformationMatrix");
        shader.connectSampler("noiseMap", 0);
        Shader multiDrawShader = new Shader(Shader.loadShaderCode("testVSMultiDraw.glsl"), Shader.loadShaderCode("testFS.glsl")).combine("pos", "vertexColor", "transformationMatrix");
        multiDrawShader.loadUniforms("projMatrix", "noiseMap", "time", "viewMatrix");
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
                .addTextureAttachment(GL40.GL_RGB8, GL11.GL_FLOAT, GL40.GL_RGB, 0)
                //color and geometry info
                .addTextureAttachment(1)
                //depth
                .addDepthTextureAttachment();
        FrameBufferObject deferredResult = new FrameBufferObject(1920, 1080, 2).addTextureAttachment(0).addTextureAttachment(1);
        deferredResult.unbind();
        FrameBufferObject combinedResult = new FrameBufferObject(1920, 1080, 1).addTextureAttachment(0).unbind();
        QuadRenderer quadRenderer = new QuadRenderer();
        GaussianBlur blurHelper = new GaussianBlur(quadRenderer);
        SSAOEffect ssao = new SSAOEffect(quadRenderer, 1920, 1080, projMatrix);
        Bloom bloom = new Bloom(deferredResult.getTextureID(1), 1920, 1080, quadRenderer);
        Combine combine = new Combine(quadRenderer);
        FXAARenderer aaRenderer = new FXAARenderer(quadRenderer, combinedResult.getTextureID(0));
        int texture = 0;

        while (!display.shouldClose()) {
            float dt = zeitgeist.getDelta();
            time += dt;
            display.pollEvents();
            if (input.isKeyDown(GLFW.GLFW_KEY_O) && System.currentTimeMillis() - lastSwitchWireframe > 100) {
                //  wireframe = !wireframe;
                lastSwitchWireframe = System.currentTimeMillis();
                world.addNextEntities(player.position);
                texture++;
                texture = texture % 3;
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
            OpenGLState.enableDepthTest();
            OpenGLState.disableAlphaBlending();
            fbo.bind();
            GL30.glClearColor(59 / 255f, 168 / 225f, 198 / 255f, 0.0f);
            fbo.clear();
            GL30.glActiveTexture(GL30.GL_TEXTURE0);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, noiseTexture);
            renderer.begin(viewMatrix, projMatrix);
            shader.loadFloat("time", time);
            renderer.render(player.getModel(), player.getTransformationMatrix());
            world.render(renderer, projMatrix, viewMatrix, ffc.position);
            multiDrawShader.bind();
            multiDrawShader.loadFloat("time", time);
            multiDrawShader.loadMatrix("projMatrix", projMatrix);
            multiDrawShader.loadMatrix("viewMatrix", viewMatrix);
            multiRenderer.render(world.getVisibleEntities(projMatrix, viewMatrix, ffc.position));
            multiDrawShader.unbind();
            fbo.unbind();
            OpenGLState.enableAlphaBlending();
            pm.render(projMatrix, viewMatrix);
            display.clear();
            OpenGLState.disableDepthTest();
            ssao.renderEffect(fbo, viewMatrix, dt);
            deferredResult.bind();
            GL30.glActiveTexture(GL30.GL_TEXTURE0);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, fbo.getDepthTexture());
            GL30.glActiveTexture(GL30.GL_TEXTURE1);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, fbo.getTextureID(0));
            GL30.glActiveTexture(GL30.GL_TEXTURE2);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, fbo.getTextureID(1));
            GL30.glActiveTexture(GL30.GL_TEXTURE3);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, ssao.getSSAOTexture());
            quadRenderer.renderDeferredLightingPass(ffc.getViewMatrix(), projMatrix, texture);
            deferredResult.unbind();
            bloom.render();
            combinedResult.bind();
            combine.render(deferredResult.getTextureID(0), bloom.getTexture());
            combinedResult.unbind();
            display.setViewport();
            aaRenderer.renderToScreen();
            display.flipBuffers();
            display.setFrameTitle("Disguised Phoenix: " + zeitgeist.getFPS() + " FPS " + " " + drawCalls + " draw calls " + df.format(facesDrawn) + " faces");
            drawCalls = 0;
            facesDrawn = 0;
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

}
