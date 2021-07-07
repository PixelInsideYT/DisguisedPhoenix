package graphics.postprocessing;

import graphics.core.objects.FrameBufferObject;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;

import static org.lwjgl.opengl.GL13.*;

public class HDRToLDR {

    private final QuadRenderer renderer;
    private final Shader resolveShader;
    private FrameBufferObject fbo;

    public HDRToLDR(int width, int height, QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory resolveFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/combine/hdrToldrFS.glsl").withAttributes("pos");
        resolveShader = resolveFactory.withUniforms("linearInputTexture").configureSampler("linearInputTexture", 0).built();
        fbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0).unbind();
    }

    public int getResult() {
        return fbo.getTextureID(0);
    }

    public void render(int input) {
        fbo.bind();
        resolveShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, input);
        renderer.renderOnlyQuad();
        resolveShader.unbind();
        fbo.unbind();
    }

}
