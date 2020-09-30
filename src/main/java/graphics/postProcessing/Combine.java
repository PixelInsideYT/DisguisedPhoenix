package graphics.postProcessing;

import graphics.objects.Shader;
import org.lwjgl.opengl.GL13;

public class Combine {

    private Shader shader;
    private QuadRenderer renderer;

    public Combine(QuadRenderer renderer){
        this.renderer=renderer;
        shader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/combine/combineFS.glsl")).combine("pos");
        shader.loadUniforms("colorTexture","bloomTexture");
        shader.connectSampler("colorTexture",0);
        shader.connectSampler("bloomTexture",1);

    }

    public void render(int lightingTexture, int bloomTexture){
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,lightingTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,bloomTexture);
        shader.bind();
        renderer.renderOnlyQuad();
        shader.unbind();
    }

}
