package graphics.postprocessing;

import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;

import static org.lwjgl.opengl.GL13.*;

public class FXAARenderer {

    private final Shader aliasShader;
    private final QuadRenderer renderer;

    public FXAARenderer(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory aliasFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/antiAliasing/FXAA.glsl").withAttributes("pos");
        aliasFactory.withUniforms("tex").configureSampler("tex", 0);
        aliasShader = aliasFactory.built();
    }


    public void render(int colorTexture) {
        aliasShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        renderer.renderOnlyQuad();
        aliasShader.unbind();
    }

}
