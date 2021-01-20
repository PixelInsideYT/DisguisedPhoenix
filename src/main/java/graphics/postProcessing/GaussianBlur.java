package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL13;

public class GaussianBlur {

    private final QuadRenderer renderer;
    private final Shader shader;

    public GaussianBlur(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory shaderFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/blur/gaussianBlurFS.glsl").withAttributes("pos");
        shader = shaderFactory.withUniforms("image", "resolution", "direction").configureSampler("image", 0).built();
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
        secondTarget.bind();
        renderer.renderOnlyQuad();
        secondTarget.unbind();
    }

}
