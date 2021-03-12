package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.OpenGLState;
import graphics.shaders.Shader;
import graphics.objects.TimerQuery;
import graphics.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

public class SSAOEffect {
    private final int width;
    private final int height;

    private final FrameBufferObject fbo;
    private final FrameBufferObject helperFbo;
    private final Shader ssaoShader;
    private final Shader blurShader;
    private final QuadRenderer renderer;
    public TimerQuery ssaoTimer;
    private boolean enabled = true;

    public SSAOEffect(QuadRenderer renderer, int width, int height, Matrix4f projMatrix) {
        this.renderer = renderer;
        this.width = width;
        this.height = height;
        fbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        helperFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        fbo.unbind();
        ShaderFactory ssaoFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/ssao/SSAOFS.glsl").withAttributes("pos");
        ssaoFactory.withUniforms("camera_positions", "projMatrixInv", "projScale", "radius", "kontrast", "sigma", "beta", "farPlane");
        ssaoFactory.configureShaderConstant("samples", 13).configureSampler("camera_positions", 0);
        ssaoShader = ssaoFactory.built();
        ssaoShader.bind();
        ssaoShader.loadMatrix("projMatrixInv", new Matrix4f(projMatrix).invert());
        Vector4f uvProjScale = projMatrix.transform(new Vector4f(1, 0, -1, 1));
        float projScale = uvProjScale.x / uvProjScale.w;
        projScale = projScale * 0.5f + 0.5f;
        projScale *= (width + height) / 2f;
        ssaoShader.loadFloat("projScale", projScale);
        ssaoShader.loadFloat("radius", 100);
        ssaoShader.unbind();
        ShaderFactory blurFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/ssao/SSAOBlur.glsl").withAttributes("pos");
        blurFactory.withUniforms("ao_in", "axis_f", "filter_scale", "edge_sharpness").configureSampler("ao_in", 0);
        blurShader = blurFactory.built();
        blurShader.bind();
        blurShader.loadInt("filter_scale", 1);
        blurShader.loadFloat("edge_sharpness", 10);

        ssaoTimer = new TimerQuery("SSAO");
    }

    public int getSSAOTexture() {
        return fbo.getTextureID(0);
    }

    public void renderEffect(FrameBufferObject gBuffer, Matrix4f projMatrix) {
        if (enabled) {
            ssaoTimer.startQuery();
            fbo.bind();
            fbo.clear(1, 1, 1, 1);
            GL30.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getDepthTexture());
            ssaoShader.bind();
            ssaoShader.loadMatrix("projMatrixInv", new Matrix4f(projMatrix).invert());
            Vector4f uvProjScale = projMatrix.transform(new Vector4f(1, 0, -1, 1));
            float projScale = uvProjScale.x / uvProjScale.w;
            projScale = projScale * 0.5f + 0.5f;
            projScale *= (width + height) / 2f;
            ssaoShader.loadFloat("projScale", projScale);
            renderer.renderOnlyQuad();
            blurSSAO();
            ssaoTimer.waitOnQuery();
        }
    }

    private void blurSSAO() {
        helperFbo.bind();
        blurShader.bind();
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.getTextureID(0));
        blurShader.load2DVector("axis_f", new Vector2f(1, 0));
        renderer.renderOnlyQuad();
        fbo.bind();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, helperFbo.getTextureID(0));
        blurShader.load2DVector("axis_f", new Vector2f(0, 1));
        renderer.renderOnlyQuad();
        blurShader.unbind();
        fbo.unbind();
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        enabled = false;
    }

    public void resize(int width, int height) {
        fbo.resize(width, height);
        helperFbo.resize(width, height);
    }
}
