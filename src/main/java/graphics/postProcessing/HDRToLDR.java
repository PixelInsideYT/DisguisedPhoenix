package graphics.postProcessing;

import graphics.objects.BufferObject;
import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;

public class HDRToLDR {

    private final QuadRenderer renderer;
    private FrameBufferObject fbo;
    private final Shader resolveShader;

    public HDRToLDR(int width, int height,QuadRenderer renderer) {
        this.renderer = renderer;
        resolveShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/combine/hdrToldrFS.glsl")).combine("pos");
        resolveShader.loadUniforms("linearInputTexture");
        fbo = new FrameBufferObject(width,height,1).addTextureAttachment(0).unbind();
    }

    public int getResult(){
        return fbo.getTextureID(0);
    }

    public void render(int input) {
        fbo.bind();
        resolveShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, input);
        renderer.renderOnlyQuad();
        resolveShader.unbind();
        fbo.unbind();
    }

}
