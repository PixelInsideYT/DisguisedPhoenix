package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.graphics.core.objects.BufferObject;
import de.thriemer.graphics.core.shaders.ComputeShader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL43;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

public class OcclusionCalculator {

    private static final int MAX_OCCLUSION_OBJECTS = 4096;
    private final BufferObject resultBuffer;
    private final ComputeShader computeShader;

    public OcclusionCalculator() {
        computeShader = new ComputeShader(ShaderFactory.loadShaderCode("compute/occlusionCompute.glsl"));
        computeShader.loadUniforms("hiZ", "projViewMatrix", "viewPortWidth", "viewPortHeight", "maxSize", "minAABB", "dimension");
        computeShader.loadBufferResource("resultBuffer", 0);
        computeShader.loadBufferResource("matrixBuffer", 1);
        resultBuffer = new BufferObject(GL43.GL_SHADER_STORAGE_BUFFER);
        resultBuffer.bufferData(new int[MAX_OCCLUSION_OBJECTS], GL_DYNAMIC_DRAW);
    }

    public List<Boolean> getVisibilityInformation(Vector3f topLeft, Vector3f botRight, int hiZTexture, BufferObject matrixBuffer, int invocations, Matrix4f projViewMatrix, int width, int height) {
        List<Boolean> rt = new ArrayList<>();
        computeShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, hiZTexture);
        computeShader.bindBuffer("resultBuffer", resultBuffer);
        computeShader.bindBuffer("matrixBuffer", matrixBuffer);
        computeShader.loadMatrix4f("projViewMatrix", projViewMatrix);
        computeShader.loadFloat("viewPortWidth", width);
        computeShader.loadFloat("viewPortHeight", height);
        computeShader.loadVec3("minAABB", botRight);
        computeShader.loadVec3("dimension", new Vector3f(topLeft).sub(botRight));
        computeShader.loadInt("maxSize", invocations);
        int dispatch = (int) Math.ceil(invocations / 32f);
        computeShader.dispatch(dispatch, 1, 1);
        computeShader.setSSBOAccessBarrier();
        int[] result = resultBuffer.getBufferContentInt(invocations);
        for (int i = 0; i < invocations; i++) {
            rt.add(result[i] == 1);
        }
        computeShader.unbind();
        return rt;
    }

}
