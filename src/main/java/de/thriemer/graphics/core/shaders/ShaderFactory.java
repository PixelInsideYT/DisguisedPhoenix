package de.thriemer.graphics.core.shaders;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL40.*;

@Slf4j
public class ShaderFactory {
    //TODO: implement an include
    public static final int VERTEX_SHADER = 0;
    public static final int GEOMETRY_SHADER = 1;
    public static final int TESSELATION_CONTROL_SHADER = 2;
    public static final int TESSELATION_EVALUATION_SHADER = 3;
    public static final int FRAGMENT_SHADER = 4;
    int shaderProgram;
    private final String[] shaderSources = new String[5];
    private final List<String> uniformNames = new ArrayList<>();
    private final Map<String, Integer> samplerTextureIdMap = new HashMap<>();
    private final Map<String, String> shaderConstants = new HashMap<>();
    private final Map<String, Integer> attributeLocationMap = new HashMap<>();

    //Shader Configuration

    public ShaderFactory(String vsName, String fsName) {
        shaderSources[VERTEX_SHADER] = loadShaderCode(vsName);
        shaderSources[FRAGMENT_SHADER] = loadShaderCode(fsName);
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
            log.error("Could not read file: {}",name);
            e.printStackTrace();
            System.exit(-1);
        }
        return shaderSource.toString();
    }

    private static int convertOwnConventionToOpenGL(int in) {
        return switch (in) {
            case 0 -> GL_VERTEX_SHADER;
            case 1 -> GL_GEOMETRY_SHADER;
            case 2 -> GL_TESS_CONTROL_SHADER;
            case 3 -> GL_TESS_EVALUATION_SHADER;
            case 4 -> GL_FRAGMENT_SHADER;
            default -> -1;
        };
    }

    public ShaderFactory addShaderStage(int shaderType, String shaderPath) {
        if (shaderSources[shaderType] != null) {
            log.warn("You will overwrite {} by placing {} into that slot",shaderSources[shaderType],shaderPath);
        }
        shaderSources[shaderType] = loadShaderCode(shaderPath);
        return this;
    }

    public ShaderFactory withAttributes(String... attributes) {
        for (String variableName : attributes)
            if (!shaderSources[VERTEX_SHADER].contains(" " + variableName + ";")) {
                log.error("Input {} not found in Shader {}",variableName,shaderSources[VERTEX_SHADER]);
                System.exit(1);
            }
        for (int i = 0; i < attributes.length; i++) {
            attributeLocationMap.put(attributes[i], i);
        }
        return this;
    }

    public ShaderFactory setAttributeLocation(String name, int location) {
        if (!shaderSources[VERTEX_SHADER].contains(" " + name + ";")) {
            log.error("Input {} not found in Shader {}",name,shaderSources[VERTEX_SHADER]);
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

    public ShaderFactory withUniformArray(String arrayName, int count) {
        for (int i = 0; i < count; i++) {
            uniformNames.add(arrayName + "[" + i + "]");
        }
        return this;
    }

    public ShaderFactory configureSampler(String name, int textureId) {
        if (!uniformNames.contains(name)) uniformNames.add(name);
        samplerTextureIdMap.put(name, textureId);
        return this;
    }


    //Shader Building

    public Shader built() {
        shaderProgram = glCreateProgram();
        Map<String, Integer> uniformLocationMap = new HashMap<>();

        List<Integer> compiledSources = compileAllShaders();
        compiledSources.forEach(i -> glAttachShader(shaderProgram, i));
        attributeLocationMap.keySet().forEach(name -> glBindAttribLocation(shaderProgram, attributeLocationMap.get(name), name));
        if (attributeLocationMap.isEmpty()) {
            log.warn("Your shader has no input variables");
        }
        glLinkProgram(shaderProgram);
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            log.error("Could not link shaders: \n{}",glGetProgramInfoLog(shaderProgram, 500));
            System.exit(-1);
        }
        //load Uniforms
        for (String u : uniformNames) {
            uniformLocationMap.put(u, loadUniform(u));
        }
        Shader shader = new Shader(shaderProgram, uniformLocationMap);
        shader.bind();
        //load texture locations
        for (Map.Entry<String,Integer> entry : samplerTextureIdMap.entrySet()) {
            glUniform1i(uniformLocationMap.get(entry.getKey()), entry.getValue());
        }
        shader.setNameTextureIdMap(samplerTextureIdMap);
        deleteUnusedSources(compiledSources);
        return shader;
    }

    private int loadUniform(String name) {
        int id = glGetUniformLocation(shaderProgram, name);
        if (id == -1) {
            log.error("Uniform {} not found",name);
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
        for (Map.Entry<String,String> entry: shaderConstants.entrySet()) {
            String name = entry.getKey();
            if (withShaderConstants.contains("#VAR " + name)) {
                withShaderConstants = withShaderConstants.replaceAll("#VAR " + name, "#define " + name + entry.getValue());
                log.info("shader value {} is: {}",name,entry.getValue());
            }
        }
        return withShaderConstants;
    }

    private int attachShader(int type, String code) {
        int shader = glCreateShader(convertOwnConventionToOpenGL(type));
        glShaderSource(shader, code);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            log.error("Something went wrong for code:\n {}\nCould not compile shader",glGetShaderInfoLog(shader, 500));
            System.exit(-1);
        }
        return shader;
    }

    private void deleteUnusedSources(List<Integer> shaderIds) {
        shaderIds.forEach(i -> {
            glDetachShader(shaderProgram, i);
            glDeleteShader(i);
        });
    }
}
