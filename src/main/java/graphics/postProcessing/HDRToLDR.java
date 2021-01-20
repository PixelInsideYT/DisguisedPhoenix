package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.lwjgl.opengl.GL13;

public class HDRToLDR {

    private final QuadRenderer renderer;
    private FrameBufferObject fbo;
    private final Shader resolveShader;

    public HDRToLDR(int width, int height,QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory resolveFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/combine/hdrToldrFS.glsl").withAttributes("pos");
        resolveShader = resolveFactory.withUniforms("linearInputTexture").configureSampler("linearInputTexture",0).built();
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
