package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;

public class Bloom {

    private final int mipLevel = 3;

    private final Shader customBlurShader;

    private final FrameBufferObject helperFbo;
    private final FrameBufferObject outFbo;
    private final QuadRenderer renderer;

    public Bloom( int width, int height, QuadRenderer renderer) {
        this.renderer = renderer;
        customBlurShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/blur/bloomBlurFS.glsl")).combine("pos");
        customBlurShader.loadUniforms("image", "mipMapLevel", "direction");
        customBlurShader.connectSampler("image",0);
        helperFbo = new FrameBufferObject(width >> mipLevel, height >> mipLevel, 1).addTextureAttachment(0);
        outFbo = new FrameBufferObject(width >> mipLevel, height >> mipLevel, 1).addTextureAttachment(0);
    }

    public void render(int highlightTexture) {
        customBlurShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, highlightTexture);
        GL45.glGenerateMipmap(GL13.GL_TEXTURE_2D);
        customBlurShader.load2DVector("direction", new Vector2f(1, 0));
        customBlurShader.loadInt("mipMapLevel",mipLevel);
        helperFbo.bind();
        renderer.renderOnlyQuad();
        helperFbo.unbind();
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, helperFbo.getTextureID(0));
        GL45.glGenerateMipmap(GL13.GL_TEXTURE_2D);
        customBlurShader.load2DVector("direction", new Vector2f(0, 1));
        customBlurShader.loadInt("mipMapLevel",0);
        outFbo.bind();
        renderer.renderOnlyQuad();
        outFbo.unbind();
    }

    public int getTexture() {
        return outFbo.getTextureID(0);
    }

}
