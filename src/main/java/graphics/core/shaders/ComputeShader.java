package graphics.core.shaders;

import graphics.core.objects.BufferObject;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.*;

@Slf4j
public class ComputeShader {
    private static final FloatBuffer matrixBuffer4f = BufferUtils.createFloatBuffer(16);
    private final Map<String, Integer> uniforms = new HashMap<>();
    private final Map<String, Integer> buffers = new HashMap<>();

    private final int shaderId;

    public ComputeShader(String shaderCode) {
        shaderId = glCreateProgram();
        int computeShader = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(computeShader, shaderCode);
        glCompileShader(computeShader);
        glAttachShader(shaderId, computeShader);
        glLinkProgram(shaderId);
        if (glGetProgrami(shaderId, GL_LINK_STATUS) == GL_FALSE) {
            log.error("Error: {} \n Could not link compute shader", glGetProgramInfoLog(shaderId, 500));
            System.exit(-1);
        }
        //shader is built and linked we can cleanup
        glDetachShader(shaderId, computeShader);
        glDeleteShader(computeShader);
    }

    public void loadUniforms(String... uniformNames) {
        for (String uniformName : uniformNames) {
            loadUniform(uniformName);
        }
    }

    public void loadUniform(String name) {
        int id = glGetUniformLocation(shaderId, name);
        uniforms.put(name, id);
        if (id == -1) {
            log.error("Uniform: {} not found!", name);
        }
    }

    public void bind() {
        glUseProgram(shaderId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void loadImage(int unit, int texture, int access, int format) {
        glBindImageTexture(unit, texture, 0, false, 0, access, format);
    }

    public void connectSampler(String samplerName, int unit) {
        glUniform1i(uniforms.get(samplerName), unit);
    }

    public void dispatch(int x, int y, int z) {
        if (x == 0 || y == 0 || z == 0)
            log.error("DON'T PUT 0 in Compute Shader dispatch");
        glDispatchCompute(x, y, z);
    }

    public void setImageAccessBarrier() {
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }

    public void setSSBOAccessBarrier() {
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }

    public void loadMatrix4f(String name, Matrix4f matrix) {
        matrixBuffer4f.clear();
        matrix.get(matrixBuffer4f);
        glUniformMatrix4fv(uniforms.get(name), false, matrixBuffer4f);
    }

    public void loadFloat(String name, float value) {
        glUniform1f(uniforms.get(name), value);
    }

    public void loadInt(String name, int value) {
        glUniform1i(uniforms.get(name), value);
    }


    public void loadVec4(String name, Vector4f value) {
        glUniform4f(uniforms.get(name), value.x, value.y, value.z, value.w);
    }

    public void loadBufferResource(String name, int bindingPoint) {
        int index = GL43.glGetProgramResourceIndex(shaderId, GL_SHADER_STORAGE_BLOCK, name);
        if (index == GL_INVALID_INDEX) {
            log.error("Buffer Resource: {} not found", name);
            System.exit(1);
        }
        glShaderStorageBlockBinding(shaderId, index, bindingPoint);
        buffers.put(name, bindingPoint);
    }

    public void bindBuffer(String name, BufferObject buffer) {
        glBindBufferBase(buffer.getTarget(), buffers.get(name), buffer.getBufferID());
    }

    public void loadVec3(String name, Vector3f vec) {
        glUniform3f(uniforms.get(name),vec.x,vec.y,vec.z);
    }
}
