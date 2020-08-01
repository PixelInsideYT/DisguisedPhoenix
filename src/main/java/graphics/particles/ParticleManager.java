package graphics.particles;

import graphics.objects.Shader;
import graphics.objects.Vao;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParticleManager {

    private List<ParticleEmitter> emitters = new ArrayList<>();
    private List<Particle> inGameParticles = new ArrayList<>();
    private Shader shader;
    private Vao particleVao;


    public ParticleManager() {
        shader = new Shader(Shader.loadShaderCode("particleVS.glsl"), Shader.loadShaderCode("particleFS.glsl")).combine("pos");
        shader.loadUniforms("viewMatrix", "projMatrix","transformationMatrix","color");
        particleVao = new Vao();
        particleVao.addDataAttributes(0, 3, new float[]{ 0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f,-0.5f, 0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, -0.5f, 0f});
        particleVao.unbind();
    }

    public void update(float dt) {
        Iterator<ParticleEmitter> emitter = emitters.iterator();
        while (emitter.hasNext()) {
            ParticleEmitter pi = emitter.next();
            inGameParticles.addAll(pi.getParticles(dt));
            if (pi.toRemove()) emitter.remove();
        }
        Iterator<Particle> particleIterator = inGameParticles.iterator();
        while (particleIterator.hasNext()) {
            Particle p = particleIterator.next();
            if (p.update(dt)) {
                particleIterator.remove();
            }
        }
    }

    public void render(Matrix4f projMatrix, Matrix4f viewMatrix) {
        Matrix4f transposedViewMatrix = new Matrix4f(viewMatrix).transpose();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        shader.bind();
        particleVao.bind();
        shader.loadMatrix("viewMatrix", viewMatrix);
        shader.loadMatrix("projMatrix", projMatrix);
        for(Particle p:inGameParticles) {
            shader.loadMatrix("transformationMatrix",p.getTransformationMatrix(transposedViewMatrix));
            shader.load4DVector("color",p.getColor());
            GL31.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 6);
        }
        particleVao.unbind();
        shader.unbind();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void addParticleEmitter(ParticleEmitter pe) {
        emitters.add(pe);
    }


}
