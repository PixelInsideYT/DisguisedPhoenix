package de.thriemer.graphics.core.shaders;


import lombok.Setter;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;

public class Shader {

    private static final List<Integer> allShaderProgramms = new ArrayList<>();
    private static final FloatBuffer matrixBuffer4f = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer matrixBuffer3f = BufferUtils.createFloatBuffer(9);

    private final int shaderProgram;
    private final Map<String, Integer> uniforms;
    @Setter
    private Map<String, Integer> nameTextureIdMap;

    protected Shader(int shader, Map<String, Integer> uniforms) {
        this.shaderProgram = shader;
        allShaderProgramms.add(shader);
        this.uniforms = uniforms;
    }

    public static void cleanUpAllShaders() {
        allShaderProgramms.forEach(GL20::glDeleteProgram);
    }

    public void bind() {
        glUseProgram(shaderProgram);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void bind2DTexture(String name, int texture) {
        int offset = nameTextureIdMap.get(name);
        glActiveTexture(GL_TEXTURE0 + offset);
        glBindTexture(GL_TEXTURE_2D, texture);
    }

    public void bind2DTextureArray(String name, int texture) {
        int offset = nameTextureIdMap.get(name);
        glActiveTexture(GL_TEXTURE0 + offset);
        glBindTexture(GL_TEXTURE_2D_ARRAY, texture);
    }

    public void loadFloat(String name, float value) {
        GL20.glUniform1f(uniforms.get(name), value);
    }

    public void loadInt(String name, int value) {
        GL20.glUniform1i(uniforms.get(name), value);
    }

    public void load2DVector(String name, Vector2f vector) {
        GL20.glUniform2f(uniforms.get(name), vector.x, vector.y);
    }

    public void load3DVector(String name, Vector3f vector) {
        GL20.glUniform3f(uniforms.get(name), vector.x, vector.y, vector.z);
    }

    public void load4DVector(String name, Vector4f vector) {
        GL20.glUniform4f(uniforms.get(name), vector.x, vector.y, vector.z, vector.w);
    }

    public void loadMatrix(String name, Matrix4f matrix) {
        matrixBuffer4f.clear();
        matrix.get(matrixBuffer4f);
        GL20.glUniformMatrix4fv(uniforms.get(name), false, matrixBuffer4f);
    }

    public void loadMatrix(String name, Matrix3f matrix) {
        matrixBuffer3f.clear();
        matrix.get(matrixBuffer3f);
        GL20.glUniformMatrix3fv(uniforms.get(name), false, matrixBuffer3f);
    }

    public void loadFloatArray(String name, float[] array) {
        Integer index = uniforms.get(name);
        if (index == null) {
            System.err.println(name + " is Null");
        }
        GL20.glUniform1fv(index, array);
    }

    public void loadVector3fArray(String name, Vector3f[] array) {
        for (int i = 0; i < array.length; i++) {
            this.load3DVector(name + "[" + i + "]", array[i]);
        }
    }

    public void loadMatrix4fArray(String name, Matrix4f[] array) {
        for (int i = 0; i < array.length; i++) {
            this.loadMatrix(name + "[" + i + "]", array[i]);
        }
    }

    public void loadIVec2(String name, Vector2i vector) {
        GL20.glUniform2i(uniforms.get(name), vector.x, vector.y);
    }
}
