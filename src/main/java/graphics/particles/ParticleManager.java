package graphics.particles;

import disuguisedPhoenix.Main;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.objects.Vbo;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParticleManager {

    private static final int INSTANCES_COUNT = 100000;
    private static final int INSTANCE_DATA_LENGTH = 20;

    private Vbo matrixAndColor;
    private FloatBuffer buffer = BufferUtils.createFloatBuffer(INSTANCES_COUNT * INSTANCE_DATA_LENGTH);

    private List<ParticleEmitter> emitters = new ArrayList<>();
    private List<Particle> inGameParticles = new ArrayList<>();
    private Shader shader;
    private Vao particleVao;


    public ParticleManager() {
        shader = new Shader(Shader.loadShaderCode("particleVS.glsl"), Shader.loadShaderCode("particleFS.glsl")).bindAttribute(0, "pos").bindAttribute(1, "transformationMatrix").bindAttribute(5, "color").combine();
        shader.loadUniforms("viewMatrix", "projMatrix");
        particleVao = new Vao("particle");
        particleVao.addDataAttributes(0, 3, new float[]{0.5f, -0.5f, 0f, 0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f, -0.5f, 0f});
        matrixAndColor = new Vbo(INSTANCES_COUNT * INSTANCE_DATA_LENGTH, GL20.GL_ARRAY_BUFFER, GL15.GL_DYNAMIC_DRAW).unbind();
        particleVao.addInstancedAttribute(matrixAndColor, 1, 4, INSTANCE_DATA_LENGTH, 0);
        particleVao.addInstancedAttribute(matrixAndColor, 2, 4, INSTANCE_DATA_LENGTH, 4);
        particleVao.addInstancedAttribute(matrixAndColor, 3, 4, INSTANCE_DATA_LENGTH, 8);
        particleVao.addInstancedAttribute(matrixAndColor, 4, 4, INSTANCE_DATA_LENGTH, 12);
        particleVao.addInstancedAttribute(matrixAndColor, 5, 4, INSTANCE_DATA_LENGTH, 16);
        particleVao.unbind();
    }

    public void update(float dt) {
        Iterator<ParticleEmitter> emitter = emitters.iterator();
        while (emitter.hasNext()) {
            ParticleEmitter pi = emitter.next();
            inGameParticles.addAll(pi.getParticles(dt));
            if (pi.toRemove()) emitter.remove();
        }
        inGameParticles.removeIf(p -> p.update(dt));
    }

    public void render(Matrix4f projMatrix, Matrix4f viewMatrix) {
        Matrix4f transposedViewMatrix = new Matrix4f(viewMatrix).transpose();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        shader.bind();
        shader.loadMatrix("viewMatrix", viewMatrix);
        shader.loadMatrix("projMatrix", projMatrix);
        particleVao.bind();
        int toRenderParticles = inGameParticles.size();
        int particleOffset = 0;
        while (toRenderParticles > 0) {
            buffer.clear();
            int bufferOffset = 0;
            int instances = Math.min(toRenderParticles, INSTANCES_COUNT);
            for (int i = 0; i < instances; i++) {
                Particle p = inGameParticles.get(i + particleOffset);
                Matrix4f m = p.getTransformationMatrix(transposedViewMatrix);
                m.get(bufferOffset, buffer);
                bufferOffset += 16;
                p.getColor().get(bufferOffset, buffer);
                bufferOffset += 4;
            }
            matrixAndColor.updateVbo(buffer);
            GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 6, instances);
            Main.drawCalls++;
            Main.facesDrawn+=2*instances;
            particleOffset += instances;
            toRenderParticles -= instances;
        }
        particleVao.unbind();
        shader.unbind();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void addParticleEmitter(ParticleEmitter pe) {
        emitters.add(pe);
    }

}
