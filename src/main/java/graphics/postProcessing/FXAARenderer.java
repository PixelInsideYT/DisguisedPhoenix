package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.lwjgl.opengl.GL13;

public class FXAARenderer {

    private final Shader aliasShader;
    private final QuadRenderer renderer;

    public FXAARenderer(int width, int height,QuadRenderer renderer) {
        this.renderer = renderer;
        aliasShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/antiAliasing/FXAA.glsl")).combine("pos");
        aliasShader.loadUniforms("tex");
        aliasShader.connectSampler("tex", 0);
    }


    public void render(int colorTexture) {
        aliasShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, colorTexture);
        renderer.renderOnlyQuad();
        aliasShader.unbind();
    }

}
