package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.lwjgl.opengl.GL13;

public class Combine {

    private final Shader shader;
    private final QuadRenderer renderer;
    private final FrameBufferObject combinedResult;

    public Combine(int width, int height,QuadRenderer renderer){
        this.renderer=renderer;
        shader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/combine/combineFS.glsl")).combine("pos");
        shader.loadUniforms("colorTexture","bloomTexture","godRaysTexture");
        shader.connectSampler("colorTexture",0);
        shader.connectSampler("bloomTexture",1);
        shader.connectSampler("godRaysTexture",2);
        combinedResult = new FrameBufferObject(width, height, 1).addUnclampedTexture(0).unbind();
    }

    public int getCombinedResult(){
        return combinedResult.getTextureID(0);
    }

    public void render(int lightingTexture, int bloomTexture, int godRaysTexture){
        combinedResult.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,lightingTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,bloomTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,godRaysTexture);
        shader.bind();
        renderer.renderOnlyQuad();
        shader.unbind();
        combinedResult.unbind();
    }

}
