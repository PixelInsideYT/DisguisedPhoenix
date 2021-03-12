package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;

public class Bloom {

    private final int mipLevel = 0;

    private final Shader customBlurShader;

    private final FrameBufferObject helperFbo;
    private final FrameBufferObject outFbo;
    private final QuadRenderer renderer;

    public Bloom( int width, int height, QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory blurFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/blur/bloomBlurFS.glsl").withAttributes("pos");
        blurFactory.withUniforms("image", "mipMapLevel", "direction");
        blurFactory.configureSampler("image",0);
        customBlurShader = blurFactory.built();
        helperFbo = new FrameBufferObject(width >> mipLevel, height >> mipLevel, 1).addTextureAttachment(0);
        outFbo = new FrameBufferObject(width >> mipLevel, height >> mipLevel, 1).addTextureAttachment(0);
    }

    public void render(int highlightTexture) {
        customBlurShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, highlightTexture);
        GL45.glGenerateMipmap(GL13.GL_TEXTURE_2D);
        customBlurShader.load2DVector("direction", new Vector2f(3, 0));
        customBlurShader.loadInt("mipMapLevel",mipLevel);
        helperFbo.bind();
        renderer.renderOnlyQuad();
        helperFbo.unbind();
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, helperFbo.getTextureID(0));
        GL45.glGenerateMipmap(GL13.GL_TEXTURE_2D);
        customBlurShader.load2DVector("direction", new Vector2f(0, 3));
        customBlurShader.loadInt("mipMapLevel",0);
        outFbo.bind();
        renderer.renderOnlyQuad();
        outFbo.unbind();
    }

    public int getTexture() {
        return outFbo.getTextureID(0);
    }

    public void resize(int width, int height) {
        helperFbo.resize(width, height);
        outFbo.resize(width, height);
    }
}
