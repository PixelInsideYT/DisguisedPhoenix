package graphics.objects;


import engine.util.BiMap;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

public class Shader {

    private static List<Integer> allShaderProgramms = new ArrayList<>();
    private static FloatBuffer matrixBuffer4f = BufferUtils.createFloatBuffer(16);
    private static FloatBuffer matrixBuffer3f = BufferUtils.createFloatBuffer(9);

    public int shaderProgram;
    private List<Integer> shaders = new ArrayList<>();
    private Map<String, Integer> uniforms = new HashMap<>();
    private String vsShaderCode;

    public Shader(String vertex, String fragment) {
        this.vsShaderCode=vertex;
        shaders.add(attachShader(GL_VERTEX_SHADER, vertex));
        shaders.add(attachShader(GL_FRAGMENT_SHADER, fragment));
    }

    public Shader(String vertex, String geometry, String fragment) {
        this.vsShaderCode = vertex;
        shaders.add(attachShader(GL_VERTEX_SHADER, vertex));
        shaders.add(attachShader(GL_GEOMETRY_SHADER, geometry));
        shaders.add(attachShader(GL_FRAGMENT_SHADER, fragment));
    }

    public static String loadShaderCode(String name) {
        StringBuilder shaderSource = new StringBuilder();
        try {
            InputStreamReader isr = new InputStreamReader(
                    Shader.class.getClassLoader().getResourceAsStream("shaders/" + name));
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                shaderSource.append(line).append("//\n");
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Could not read file. " + name);
            e.printStackTrace();
            System.exit(-1);
        }
        return shaderSource.toString();
    }

    public static void cleanUpAllShaders() {
        allShaderProgramms.forEach(GL20::glDeleteProgram);
    }

    private int attachShader(int type, String code) {
        int shader = glCreateShader(type);
        glShaderSource(shader, code);
        glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.out.println("Something went wrong for code: " + code + " \n");
            System.out.println(GL20.glGetShaderInfoLog(shader, 500));
            System.err.println("Could not compile shader.");
            System.exit(-1);

        }
        return shader;
    }

    public Shader combine(String... attributes) {
        shaderProgram = glCreateProgram();
        allShaderProgramms.add(shaderProgram);
        shaders.forEach(i -> glAttachShader(shaderProgram, i));
        bindAtributs(attributes);
        glLinkProgram(shaderProgram);
        if (GL20.glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(GL20.glGetProgramInfoLog(shaderProgram, 500));
            System.err.println("Could not links shaders.");
            System.exit(-1);
        }
        deleteShaders();
        return this;
    }

    public void bind() {
        glUseProgram(shaderProgram);
    }

    public void unbind() {
        glUseProgram(0);
    }

    private void bindAtributs(String... attributes) {
        if (attributes.length != 0) {
            for (int i = 0; i < attributes.length; i++) {
                bindAttribute(i, attributes[i]);
            }
        } else {
            nameLocationMap.getSet().forEach(name -> bindAttribute(nameLocationMap.get(name), name));
        }
    }

    private void bindAttribute(int attribute, String variableName) {
        if(!vsShaderCode.contains(" "+variableName+";")){
            System.err.println("Input: "+variableName+"; not found in Shader:\n"+vsShaderCode);
            System.exit(1);
        }
        GL20.glBindAttribLocation(shaderProgram, attribute, variableName);
    }

    private BiMap<String, Integer> nameLocationMap = new BiMap<>();

    public Shader configureAttribute(int attribute, String variableName) {
        nameLocationMap.put(variableName, attribute);
        return this;
    }

    public Shader connectSampler(String samplerName, int unit) {
        bind();
        GL20.glUniform1i(uniforms.get(samplerName), unit);
        unbind();
        return this;
    }

    public void loadUniforms(String... uniformNames) {
        for (String uniformName : uniformNames) {
            loadUniform(uniformName);
        }
    }

    public void loadUniform(String name) {
        int id = GL20.glGetUniformLocation(shaderProgram, name);
        uniforms.put(name, id);
        if (id == -1) {
            System.err.println("Uniform: " + name + " not found!");
        }
    }

    public void loadUniformArray(String arrayName,int count){
        for(int i=0;i<count;i++){
            loadUniform(arrayName+"["+i+"]");
        }
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
        GL20.glUniform1fv(uniforms.get(name), array);
    }

    public void loadVector3fArray(String name, Vector3f[] array) {
        for(int i=0;i<array.length;i++){
            this.load3DVector(name+"["+i+"]",array[i]);
        }
    }

    private void deleteShaders() {
        unbind();
        shaders.forEach(i -> {
            glDetachShader(shaderProgram, i);
            glDeleteShader(i);
        });
    }

}
