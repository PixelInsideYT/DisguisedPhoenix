package de.thriemer.graphics.postprocessing;

import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.objects.GPUTimerQuery;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import org.joml.Vector2f;

import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL45.glGenerateMipmap;

public class Bloom {

    private static final String MIP_LEVEL = "mipMapLevel";
    private static final String DIRECTION = "direction";
    private final Shader customBlurShader;
    private final FrameBufferObject helperFbo;
    private final FrameBufferObject outFbo;
    private final QuadRenderer renderer;
    private int mipLevel = 0;
    private GPUTimerQuery bloomTimer = new GPUTimerQuery("Bloom");

    public Bloom(int width, int height, QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory blurFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/blur/bloomBlurFS.glsl").withAttributes("pos");
        blurFactory.withUniforms("image", MIP_LEVEL, DIRECTION);
        blurFactory.configureSampler("image", 0);
        customBlurShader = blurFactory.built();
        helperFbo = new FrameBufferObject(width >> mipLevel, height >> mipLevel, 1).addTextureAttachment(0);
        outFbo = new FrameBufferObject(width >> mipLevel, height >> mipLevel, 1).addTextureAttachment(0);
    }

    public void render(int highlightTexture) {
        bloomTimer.startQuery();
        customBlurShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, highlightTexture);
        glGenerateMipmap(GL_TEXTURE_2D);
        customBlurShader.load2DVector(DIRECTION, new Vector2f(3, 0));
        customBlurShader.loadInt(MIP_LEVEL, mipLevel);
        helperFbo.bind();
        renderer.renderOnlyQuad();
        helperFbo.unbind();
        glBindTexture(GL_TEXTURE_2D, helperFbo.getTextureID(0));
        glGenerateMipmap(GL_TEXTURE_2D);
        customBlurShader.load2DVector(DIRECTION, new Vector2f(0, 3));
        customBlurShader.loadInt(MIP_LEVEL, 0);
        outFbo.bind();
        renderer.renderOnlyQuad();
        outFbo.unbind();
        bloomTimer.stopQuery();
    }

    public int getTexture() {
        return outFbo.getTextureID(0);
    }

    public void resize(int width, int height) {
        helperFbo.resize(width, height);
        outFbo.resize(width, height);
    }
}
