package graphics.objects;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;

public class ComputeShader {
    private static final FloatBuffer matrixBuffer4f = BufferUtils.createFloatBuffer(16);
    private final Map<String, Integer> uniforms = new HashMap<>();
    private final int shaderId;

    public ComputeShader(String shaderCode) {
        shaderId = GL20.glCreateProgram();
        int computeShader = GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        GL20.glShaderSource(computeShader, shaderCode);
        GL20.glAttachShader(shaderId, computeShader);
        GL20.glLinkProgram(shaderId);
        if (GL20.glGetProgrami(shaderId, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(GL20.glGetProgramInfoLog(shaderId, 500));
            System.err.println("Could not links compute Shader.");
            System.exit(-1);
        }
        //shader is built and linked we can cleanup
        GL20.glDetachShader(shaderId, computeShader);
        GL20.glDeleteShader(computeShader);
    }

    public void loadUniforms(String... uniformNames) {
        for (String uniformName : uniformNames) {
            loadUniform(uniformName);
        }
    }

    public void loadUniform(String name) {
        int id = GL20.glGetUniformLocation(shaderId, name);
        uniforms.put(name, id);
        if (id == -1) {
            System.err.println("Uniform: " + name + " not found!");
        }
    }

    public void bind() {
        GL20.glUseProgram(shaderId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public void loadImage(int unit,int texture, int access,int format){
        GL43.glBindImageTexture(unit,texture,0,false,0, access,format);
    }

    public void connectSampler(String samplerName, int unit) {
        GL20.glUniform1i(uniforms.get(samplerName), unit);
    }

    public void run(int x, int y, int z) {
        GL43.glDispatchCompute(x, y, z);
    }
    public void setImageAccesBarrier(){
        GL43.glMemoryBarrier(GL43.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }
    public void setSSBOAccesBarrier(){GL43.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);}
    public boolean isFinished() {
        return true;
    }


    public void loadMatrix4f(String name, Matrix4f matrix) {
        matrixBuffer4f.clear();
        matrix.get(matrixBuffer4f);
        GL20.glUniformMatrix4fv(uniforms.get(name), false, matrixBuffer4f);
    }

    public void loadFloat(String name, float value) {
        GL20.glUniform1f(uniforms.get(name), value);
    }

}
