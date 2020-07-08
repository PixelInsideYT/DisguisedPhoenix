package graphics.particles;

import disuguisedPhoenix.terrain.Terrain;
import engine.input.InputManager;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import engine.util.Zeitgeist;
import graphics.camera.FreeFlightCamera;
import graphics.context.Display;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.renderer.TestRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;

public class ParticleTester {



    public static void main(String args[]) throws InterruptedException {
        Display display = new Display(1080, 720);
        Zeitgeist zeitgeist = new Zeitgeist();
        ParticleManager pm = new ParticleManager();
        while (!display.shouldClose()) {
            float dt = zeitgeist.getDelta();
            display.pollEvents();
            display.clear();
            pm.render(null,null);
            display.flipBuffers();
            Thread.sleep(500);
        }
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }



}
