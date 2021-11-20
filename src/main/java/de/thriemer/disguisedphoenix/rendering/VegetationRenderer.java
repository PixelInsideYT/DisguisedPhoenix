package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.graphics.core.objects.Vao;
import de.thriemer.graphics.core.renderer.MultiIndirectRenderer;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import de.thriemer.graphics.loader.TextureLoader;
import de.thriemer.graphics.modelinfo.RenderInfo;
import lombok.Getter;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class VegetationRenderer {

    private final MultiIndirectRenderer multiRenderer;
    protected Shader vegetationShader;
    @Getter
    private int windTexture;

    public VegetationRenderer(MultiIndirectRenderer multiIndirectRenderer) {
        this.multiRenderer = multiIndirectRenderer;
        ShaderFactory shaderFactory = new ShaderFactory("testVSMultiDraw.glsl", "testFS.glsl").withAttributes("posAndWobble", "colorAndShininess");
        shaderFactory.withUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "transformationMatrixUniform", "useInputTransformationMatrix");
        vegetationShader = shaderFactory.configureSampler("noiseMap", 0).built();
        windTexture = TextureLoader.loadTexture("misc/noiseMap.png", GL_REPEAT, GL_LINEAR);
    }

    public void prepareRender(Map<Vao, Map<RenderInfo, List<Matrix4f>>> vaoSortedEntries){
        multiRenderer.prepareRenderer(vaoSortedEntries);
    }

    public void render(float time, CameraInformation cameraInformation){
        vegetationShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, windTexture);
        vegetationShader.loadMatrix("projMatrix", cameraInformation.getProjectionMatrix());
        vegetationShader.loadMatrix("viewMatrix", cameraInformation.getViewMatrix());
        vegetationShader.loadFloat("time", time);
        vegetationShader.loadInt("useInputTransformationMatrix", 1);
        multiRenderer.render();
        vegetationShader.unbind();
    }

}
