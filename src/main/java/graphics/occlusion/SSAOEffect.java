package graphics.occlusion;

import disuguisedphoenix.Main;
import disuguisedphoenix.rendering.MasterRenderer;
import graphics.core.objects.FrameBufferObject;
import graphics.core.objects.TimerQuery;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;
import graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import static org.lwjgl.opengl.GL30.glActiveTexture;

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
        ssaoShader.loadFloat("radius", 0.05f);
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

    public void renderEffect(FrameBufferObject gBuffer, Matrix4f projMatrix, float farPlane) {
        if (enabled) {
            ssaoTimer.startQuery();
            fbo.bind();
            fbo.clear(1, 1, 1, 1);
            glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getDepthTexture());
            ssaoShader.bind();
            ssaoShader.loadMatrix("projMatrixInv", new Matrix4f(projMatrix).invert());
            ssaoShader.loadFloat("farPlane", farPlane);
            ssaoShader.loadFloat("projScale", calculateProjectionScale(projMatrix));
            renderer.renderOnlyQuad();
            blurSSAO();
            ssaoTimer.waitOnQuery();
        }
    }

    private void blurSSAO() {
        helperFbo.bind();
        blurShader.bind();
        glActiveTexture(GL13.GL_TEXTURE0);
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

    private float calculateProjectionScale(Matrix4f projMatrix) {
        Vector4f uvProjScale = projMatrix.transform(new Vector4f(0, 1, -1, 1));
        float projScale = uvProjScale.y / uvProjScale.w;
        projScale = projScale * 0.5f + 0.5f;
        projScale *= Math.max(width, height);
        return projScale;
    }
}
