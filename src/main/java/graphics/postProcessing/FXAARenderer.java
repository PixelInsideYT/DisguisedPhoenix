package graphics.postProcessing;

import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.lwjgl.opengl.GL13;

public class FXAARenderer {

    private final Shader aliasShader;
    private final QuadRenderer renderer;

    public FXAARenderer(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory aliasFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/antiAliasing/FXAA.glsl").withAttributes("pos");
        aliasFactory.withUniforms("tex").configureSampler("tex", 0);
        aliasShader = aliasFactory.built();
    }


    public void render(int colorTexture) {
        aliasShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, colorTexture);
        renderer.renderOnlyQuad();
        aliasShader.unbind();
    }

}
