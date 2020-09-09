package graphics.postProcessing.SSAO;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import graphics.postProcessing.GaussianBlur;
import graphics.postProcessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

public class SSAOEffect {
    private int width, height;

    private FrameBufferObject fbo;
    private FrameBufferObject temporalFbo;
    private FrameBufferObject helperFbo;
    private Shader ssaoShader;
    private Shader temporalShader;
    private Shader blurShader;
    private Matrix4f projMatrix;

    private QuadRenderer renderer;
    private GaussianBlur blur;

    public SSAOEffect(QuadRenderer renderer, GaussianBlur blurHelper, int width, int height, Matrix4f projMatrix) {
        this.renderer = renderer;
        this.blur=blurHelper;
        this.width = width;
        this.height = height;
        fbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        temporalFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0).addDepthTextureAttachment();
        helperFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        fbo.unbind();
        ssaoShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/SSAOFS.glsl")).combine("pos");
        ssaoShader.loadUniforms("camera_positions", "camera_normals", "n_samples", "turns", "ball_radius", "sigma", "kappa", "rnd");
        ssaoShader.connectSampler("camera_positions", 0);
        ssaoShader.connectSampler("camera_normals", 1);
        ssaoShader.bind();
        ssaoShader.loadInt("n_samples", 11);
        ssaoShader.loadInt("turns", 11);
        ssaoShader.loadFloat("ball_radius", 10f);
        ssaoShader.loadFloat("sigma", 20f);
        ssaoShader.loadFloat("kappa", 2f);
        ssaoShader.unbind();
        blurShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/SSAOBlur.glsl")).combine("pos");
        blurShader.loadUniforms("ao_in", "axis","filter_scale","edge_sharpness");
        blurShader.connectSampler("ao_in", 0);
        blurShader.bind();
        blurShader.loadInt("filter_scale",1);
        blurShader.loadFloat("edge_sharpness",10);
        temporalShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/temporalFS.glsl")).combine("pos");
        temporalShader.loadUniforms("positionTexture", "currentSSAO", "lastSSAO", "lastDepth", "velocity", "reprojectMatrix", "clipInfo","invViewMatrix", "projInfo", "projMatrixInv");
        temporalShader.connectSampler("positionTexture", 0);
        temporalShader.connectSampler("currentSSAO", 1);
        temporalShader.connectSampler("lastSSAO", 2);
        temporalShader.connectSampler("lastDepth", 3);
        temporalShader.connectSampler("velocity", 4);
        temporalShader.bind();
        float nearPlane = 1;
        float farPlane = 10000;
        temporalShader.load3DVector("clipInfo", new Vector3f(nearPlane * farPlane, nearPlane - farPlane, farPlane));
        temporalShader.load4DVector("projInfo",
                new Vector4f(-2.0f / (width * projMatrix.get(0, 0)),
                        -2.0f / (height * projMatrix.get(1, 1)),
                        (1.0f - projMatrix.get(0, 2)) / projMatrix.get(0, 0),
                        (1.0f + projMatrix.get(1, 2)) / projMatrix.get(1, 1)));
        temporalShader.loadMatrix("projMatrixInv", new Matrix4f(projMatrix).invert());
        temporalShader.unbind();
        this.projMatrix = projMatrix;
    }

    public int getSSAOTexture() {
        return fbo.getTextureID(0);
    }


    public void renderEffect(FrameBufferObject gBuffer, Matrix4f viewMatrix, float dt) {
        fbo.bind();
        fbo.clear(1, 1, 1, 1);
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getTextureID(0));
        GL30.glGenerateMipmap(GL13.GL_TEXTURE_2D);
        GL30.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getTextureID(1));
        ssaoShader.bind();
        ssaoShader.loadFloat("rnd", (float) Math.random() * 16f);
        renderer.renderOnlyQuad();
        ssaoShader.unbind();
        fbo.unbind();
        renderTemporalShader(gBuffer, viewMatrix, dt);
        //blurSSAO();
        blur.blur(temporalFbo.getTextureID(0),width,height,helperFbo,fbo,2);
    }


    private void blurSSAO() {
        helperFbo.bind();
        blurShader.bind();
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, temporalFbo.getTextureID(0));
        blurShader.load2DVector("axis", new Vector2f(1, 0));
        renderer.renderOnlyQuad();
        helperFbo.unbind();
        fbo.bind();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, helperFbo.getTextureID(0));
        blurShader.load2DVector("axis", new Vector2f(0, 1));
        renderer.renderOnlyQuad();
        blurShader.unbind();
        fbo.unbind();
    }

    private Matrix4f lastViewMatrix = new Matrix4f();

    private void renderTemporalShader(FrameBufferObject gBuffer, Matrix4f viewMatrix, float dt) {
        temporalShader.bind();
        helperFbo.bind();
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getTextureID(0));
        GL30.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fbo.getTextureID(0));
        GL30.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, temporalFbo.getTextureID(0));
        GL30.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, temporalFbo.getDepthTexture());
        GL30.glActiveTexture(GL13.GL_TEXTURE4);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getTextureID(3));

        Matrix4f reprojectMatrix = new Matrix4f(projMatrix).mul(lastViewMatrix);
        temporalShader.loadMatrix("reprojectMatrix", reprojectMatrix);
        temporalShader.loadMatrix("invViewMatrix",new Matrix4f(viewMatrix).invert());
        renderer.renderOnlyQuad();
        temporalShader.unbind();
        helperFbo.unbind();
        helperFbo.blitToFbo(temporalFbo);
        gBuffer.blitDepth(temporalFbo);
        this.lastViewMatrix.set(viewMatrix);
    }

}
