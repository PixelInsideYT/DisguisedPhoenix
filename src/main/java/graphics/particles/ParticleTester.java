package graphics.particles;

import engine.util.Zeitgeist;
import graphics.context.Display;
import graphics.objects.Shader;
import graphics.objects.Vao;

public class ParticleTester {


    public static void main(String[] args) throws InterruptedException {
        Display display = new Display(1080, 720);
        Zeitgeist zeitgeist = new Zeitgeist();
        ParticleManager pm = new ParticleManager();
        while (!display.shouldClose()) {
            float dt = zeitgeist.getDelta();
            display.pollEvents();
            display.clear();
            pm.render(null, null);
            display.flipBuffers();
            Thread.sleep(500);
        }
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }


}
