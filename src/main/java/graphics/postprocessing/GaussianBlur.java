package graphics.postprocessing;

import graphics.objects.FrameBufferObject;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Vector2f;

import static org.lwjgl.opengl.GL13.*;

public class GaussianBlur {

    private static final String RESOLUTION = "resolution";
    private static final String DIRECTION = "direction";


    private final QuadRenderer renderer;
    private final Shader shader;

    public GaussianBlur(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory shaderFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/blur/gaussianBlurFS.glsl").withAttributes("pos");
        shader = shaderFactory.withUniforms("image", RESOLUTION, DIRECTION).configureSampler("image", 0).built();
    }

    public void blur(int inputTexture, int width, int height, FrameBufferObject firstTarget, FrameBufferObject secondTarget, int radius) {
        shader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTexture);
        shader.load2DVector(RESOLUTION, new Vector2f(width, height));
        shader.load2DVector(DIRECTION, new Vector2f(radius, 0));
        firstTarget.bind();
        renderer.renderOnlyQuad();
        firstTarget.unbind();
        glBindTexture(GL_TEXTURE_2D, firstTarget.getTextureID(0));
        shader.load2DVector(DIRECTION, new Vector2f(0, radius));
        shader.load2DVector(RESOLUTION,new Vector2f(firstTarget.getBufferWidth(),firstTarget.getBufferHeight()));
        secondTarget.bind();
        renderer.renderOnlyQuad();
        secondTarget.unbind();
    }

}
