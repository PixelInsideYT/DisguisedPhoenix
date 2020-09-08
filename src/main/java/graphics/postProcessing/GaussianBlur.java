package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL13;

public class GaussianBlur {

    private QuadRenderer renderer;
    private Shader shader;

    public GaussianBlur(QuadRenderer renderer) {
        this.renderer = renderer;
        shader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/blur/gaussianBlurFS.glsl")).combine("pos");
        shader.loadUniforms("image", "resolution", "direction");
        shader.connectSampler("image", 0);
    }

    public void blur(int inputTexture, int width, int height, FrameBufferObject firstTarget, FrameBufferObject secondTarget, int radius) {
        shader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, inputTexture);
        shader.load2DVector("resolution", new Vector2f(width, height));
        shader.load2DVector("direction", new Vector2f(radius, 0));
        firstTarget.bind();
        renderer.renderOnlyQuad();
        firstTarget.unbind();
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, firstTarget.getTextureID(0));
        shader.load2DVector("direction", new Vector2f(0, radius));
        shader.load2DVector("resolution",new Vector2f(firstTarget.getBufferWidth(),firstTarget.getBufferHeight()));
        if(secondTarget!=null)
        secondTarget.bind();
        renderer.renderOnlyQuad();
        if(secondTarget!=null)
        secondTarget.unbind();
    }

}
