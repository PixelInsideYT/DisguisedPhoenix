package graphics.shaders;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import static org.lwjgl.opengl.GL20.*;

public class ShaderFactory {
    public static final int VERTEX_SHADER = 0;
    public static final int GEOMETRY_SHADER = 1;
    public static final int TESSELATION_CONTROL_SHADER = 2;
    public static final int TESSELATION_EVALUATION_SHADER = 3;
    public static final int FRAGMENT_SHADER = 4;
//TODO: bind textures to name not to int
    int shaderProgram;
    private String[] shaderSources = new String[5];
    private List<String> uniformNames = new ArrayList<>();
    private Map<String, Integer> samplerTextureIdMap = new HashMap<>();
    private Map<String, String> shaderConstants = new HashMap<>();
    private Map<String, Integer> attributeLocationMap = new HashMap<>();

    //Shader Configuration

    public ShaderFactory(String vsName, String fsName) {
        shaderSources[VERTEX_SHADER] = loadShaderCode(vsName);
        shaderSources[FRAGMENT_SHADER] = loadShaderCode(fsName);
    }

    public ShaderFactory addShaderStage(int shaderType, String shaderPath) {
        if (shaderSources[shaderType] != null) {
            System.err.println("WARNING: You will overwrite:\n " + shaderSources[shaderType] + "\n by placing: " + shaderPath + " into that slot");
        }
        shaderSources[shaderType] = loadShaderCode(shaderPath);
        return this;
    }

    public ShaderFactory withAttributes(String... attributes) {
        for (String variableName : attributes)
            if (!shaderSources[VERTEX_SHADER].contains(" " + variableName + ";")) {
                System.err.println("Input: " + variableName + "; not found in Shader:\n" + shaderSources[VERTEX_SHADER]);
                System.exit(1);
            }
        for (int i = 0; i < attributes.length; i++) {
            attributeLocationMap.put(attributes[i], i);
        }
        return this;
    }

    public ShaderFactory setAttributeLocation(String name, int location) {
        if (!shaderSources[VERTEX_SHADER].contains(" " + name + ";")) {
            System.err.println("Input: " + name + "; not found in Shader:\n" + shaderSources[VERTEX_SHADER]);
            System.exit(1);
        }
        attributeLocationMap.put(name, location);
        return this;
    }

    public ShaderFactory configureShaderConstant(String constantName, int value) {
        return configureShaderConstant(constantName, " " + value);
    }

    public ShaderFactory configureShaderConstant(String constantName, float value) {
        return configureShaderConstant(constantName, " " + value);
    }

    private ShaderFactory configureShaderConstant(String constantName, String value) {
        shaderConstants.put(constantName, value);
        return this;
    }

    public ShaderFactory withUniforms(String... uniformNamesArray) {
        Collections.addAll(uniformNames, uniformNamesArray);
        return this;
    }


    public ShaderFactory withUniformArray(String arrayName,int count){
        for(int i=0;i<count;i++){
            uniformNames.add(arrayName+"["+i+"]");
        }
        return this;
    }

    public ShaderFactory configureSampler(String name, int textureId) {
        if (!uniformNames.contains(name)) uniformNames.add(name);
        samplerTextureIdMap.put(name, textureId);
        return this;
    }

    public static String loadShaderCode(String name) {
        StringBuilder shaderSource = new StringBuilder();
        try {
            InputStreamReader isr = new InputStreamReader(
                    ShaderFactory.class.getClassLoader().getResourceAsStream("shaders/" + name));
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

    public Shader built() {
        shaderProgram = glCreateProgram();
        Map<String, Integer> uniformLocationMap = new HashMap<>();

        List<Integer> compiledSources = compileAllShaders();
        compiledSources.forEach(i -> glAttachShader(shaderProgram, i));
        attributeLocationMap.keySet().forEach(name -> glBindAttribLocation(shaderProgram, attributeLocationMap.get(name), name));
        if(attributeLocationMap.keySet().size()==0){
            System.err.println("WARNING: Your shader has no input Variables!");
        }
        glLinkProgram(shaderProgram);
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            System.out.println(glGetProgramInfoLog(shaderProgram, 500));
            System.err.println("Could not link shaders.");
            System.exit(-1);
        }
        //load Uniforms
        for(String u:uniformNames){
            uniformLocationMap.put(u,loadUniform(u));
        }
        Shader shader = new Shader(shaderProgram,uniformLocationMap);
        shader.bind();
        //load texture locations
        for(String name:samplerTextureIdMap.keySet()){
            glUniform1i(uniformLocationMap.get(name), samplerTextureIdMap.get(name));
        }
        deleteUnusedSources(compiledSources);
        return shader;
    }


    //Shader Building

    private int loadUniform(String name) {
        int id = glGetUniformLocation(shaderProgram, name);
        if (id == -1) {
            System.err.println("Uniform: " + name + " not found!");
        }
        return id;
    }

    private List<Integer> compileAllShaders() {
        List<Integer> shaderIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (shaderSources[i] != null) {
                String processedShaderSource = setShaderConstants(shaderSources[i]);
                shaderIds.add(attachShader(i, processedShaderSource));
            }
        }
        return shaderIds;
    }

    private String setShaderConstants(String sourceCode) {
        String withShaderConstants = sourceCode;
        for(String name:shaderConstants.keySet()){
            if(withShaderConstants.contains("#VAR "+name)) {
                withShaderConstants = withShaderConstants.replaceAll("#VAR "+name,"#define "+name+shaderConstants.get(name));
                System.out.println("Info: Shadervalue "+name+" is:"+shaderConstants.get(name));
            }
        }
        return withShaderConstants;
    }

    private int attachShader(int type, String code) {
        int shader = glCreateShader(convertOwnConventionToOpenGL(type));
        glShaderSource(shader, code);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.out.println("Something went wrong for code: " + code + " \n");
            System.out.println(glGetShaderInfoLog(shader, 500));
            System.err.println("Could not compile shader.");
            System.exit(-1);
        }
        return shader;
    }

    private static int convertOwnConventionToOpenGL(int in) {
        switch (in) {
            case 0:
                return GL_VERTEX_SHADER;
            case 1:
                return GL40.GL_GEOMETRY_SHADER;
            case 2:
                return GL40.GL_TESS_CONTROL_SHADER;
            case 3:
                return GL40.GL_TESS_EVALUATION_SHADER;
            case 4:
                return GL_FRAGMENT_SHADER;
        }
        return -1;
    }

    private void deleteUnusedSources(List<Integer> shaderIds) {
        shaderIds.forEach(i -> {
            glDetachShader(shaderProgram, i);
            glDeleteShader(i);
        });
    }
}
