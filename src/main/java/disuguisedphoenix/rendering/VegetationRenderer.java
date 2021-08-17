package disuguisedphoenix.rendering;

import disuguisedphoenix.Entity;
import graphics.core.renderer.MultiIndirectRenderer;
import graphics.core.renderer.TestRenderer;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;
import graphics.loader.TextureLoader;
import org.joml.Matrix4f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class VegetationRenderer {

    private MultiIndirectRenderer multiRenderer;
    protected Shader vegetationShader;
    public int windTexture;

    public VegetationRenderer(MultiIndirectRenderer multiIndirectRenderer) {
        this.multiRenderer = multiIndirectRenderer;
        ShaderFactory shaderFactory = new ShaderFactory("testVSMultiDraw.glsl", "testFS.glsl").withAttributes("posAndWobble", "colorAndShininess");
        shaderFactory.withUniforms("projMatrix", "noiseMap", "time", "viewMatrix", "transformationMatrixUniform", "useInputTransformationMatrix");
        vegetationShader = shaderFactory.configureSampler("noiseMap", 0).built();
        windTexture = TextureLoader.loadTexture("misc/noiseMap.png", GL_REPEAT, GL_LINEAR);
    }

    public void render(float time, Matrix4f projMatrix, Matrix4f viewMatrix, List<Entity> toRender){
        vegetationShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, windTexture);
        vegetationShader.loadMatrix("projMatrix", projMatrix);
        vegetationShader.loadMatrix("viewMatrix", viewMatrix);
        vegetationShader.loadFloat("time", time);
        vegetationShader.loadInt("useInputTransformationMatrix", 1);
        multiRenderer.prepareRenderer(toRender);
        multiRenderer.render();
        vegetationShader.unbind();
    }

}
