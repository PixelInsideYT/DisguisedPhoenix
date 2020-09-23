package graphics.postProcessing;

import graphics.objects.Shader;
import org.lwjgl.opengl.GL13;

public class FXAARenderer {

    private Shader aliasShader;
    private QuadRenderer renderer;
    private int colorTexture;

    public FXAARenderer(QuadRenderer renderer, int colorTexture) {
        this.renderer = renderer;
        this.colorTexture = colorTexture;
        aliasShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/antiAliasing/FXAA.glsl")).combine("pos");
        aliasShader.loadUniforms("tex");
        aliasShader.connectSampler("tex", 0);
    }

    public void renderToScreen() {
        aliasShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, colorTexture);
        renderer.renderOnlyQuad();
        aliasShader.unbind();
    }

}
