package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import graphics.objects.TimerQuery;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

public class SSAOEffect {
    private final int width;
    private final int height;

    private final FrameBufferObject fbo;
    private final FrameBufferObject helperFbo;
    private final Shader ssaoShader;
    private final Shader blurShader;
    private final QuadRenderer renderer;

    private boolean enabled = false;

    public SSAOEffect(QuadRenderer renderer, int width, int height, Matrix4f projMatrix) {
        this.renderer = renderer;
        this.width = width;
        this.height = height;
        fbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        helperFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        fbo.unbind();
        ssaoShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/SSAOFS.glsl")).combine("pos");
        ssaoShader.loadUniforms("camera_positions", "projMatrixInv", "projScale", "radius", "samples", "kontrast", "sigma", "beta", "farPlane");
        ssaoShader.connectSampler("camera_positions", 0);
        ssaoShader.bind();
        ssaoShader.loadMatrix("projMatrixInv", new Matrix4f(projMatrix).invert());
        Vector4f uvProjScale = projMatrix.transform(new Vector4f(1, 0, -1, 1));
        float projScale = uvProjScale.x / uvProjScale.w;
        projScale = projScale * 0.5f + 0.5f;
        projScale *= (width + height) / 2f;
        ssaoShader.loadFloat("projScale", projScale);
        ssaoShader.loadFloat("radius", 100);
        ssaoShader.loadFloat("samples", 25);
        ssaoShader.unbind();
        blurShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/SSAOBlur.glsl")).combine("pos");
        blurShader.loadUniforms("ao_in", "axis_f", "filter_scale", "edge_sharpness");
        blurShader.connectSampler("ao_in", 0);
        blurShader.bind();
        blurShader.loadInt("filter_scale", 1);
        blurShader.loadFloat("edge_sharpness", 10);
    }

    public int getSSAOTexture() {
        return fbo.getTextureID(0);
    }

    public void renderEffect(FrameBufferObject gBuffer) {
        if (enabled) {
            fbo.bind();
            fbo.clear(1, 1, 1, 1);
            GL30.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getDepthTexture());
            GL30.glGenerateMipmap(GL13.GL_TEXTURE_2D);
            ssaoShader.bind();
            renderer.renderOnlyQuad();
            ssaoShader.unbind();
            fbo.unbind();
            blurSSAO();
        }
    }

    private void blurSSAO() {
        helperFbo.bind();
        blurShader.bind();
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.getTextureID(0));
        blurShader.load2DVector("axis_f", new Vector2f(1, 0));
        renderer.renderOnlyQuad();
        helperFbo.unbind();
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

}
