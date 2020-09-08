package graphics.postProcessing.SSAO;

import graphics.loader.TextureLoader;
import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import graphics.postProcessing.GaussianBlur;
import graphics.postProcessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Random;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

public class SSAOEffect {

    private int width, height;

    private Vector3f[] kernel;
    private FrameBufferObject fbo;
    private FrameBufferObject helperFbo;
    private Shader ssaoShader;
    private GaussianBlur blurHelper;


    private QuadRenderer renderer;
    private int rotationTexture;
    private int kernelSize = 11;

    public SSAOEffect(QuadRenderer renderer,GaussianBlur blurHelper, int width, int height) {
        this.renderer = renderer;
        this.blurHelper = blurHelper;
        this.width =width;
        this.height=height;
        fbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        helperFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        fbo.unbind();
        ssaoShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/SSAOFS.glsl")).combine("pos");
        ssaoShader.loadUniforms("projection", "viewPosTexture", "normalAndShininessTexture", "texNoise");
        ssaoShader.loadUniformArray("kernel", kernelSize);
        ssaoShader.connectSampler("viewPosTexture", 0);
        ssaoShader.connectSampler("normalAndShininessTexture", 1);
        ssaoShader.connectSampler("texNoise", 2);
        createKernel(kernelSize);
        ssaoShader.bind();
        ssaoShader.loadVector3fArray("kernel", kernel);
        ssaoShader.unbind();
        rotationTexture = createRotationTexture();
    }

    public int getRotationTexture() {
        return rotationTexture;
    }

    public int getSSAOTexture() {
        return fbo.getTextureID(0);
    }

    public void renderEffect(Matrix4f projection, FrameBufferObject gBuffer) {
        fbo.bind();
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getTextureID(0));
        GL30.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, gBuffer.getTextureID(1));
        GL30.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, rotationTexture);
        ssaoShader.bind();
        ssaoShader.loadMatrix("projection", projection);
        renderer.renderOnlyQuad();
        ssaoShader.unbind();
        fbo.unbind();
        blurHelper.blur(fbo.getTextureID(0),width,height,helperFbo,fbo,1);
    }


    private void createKernel(int count) {
        Random r = new Random();
        kernel = new Vector3f[count];
        for (int i = 0; i < count; i++) {
            Vector3f vec = new Vector3f(r.nextFloat() * 2f - 1f, r.nextFloat() * 2f - 1f, r.nextFloat() * 2f - 1f);
            vec.normalize();
            float scale = (float) i / (float) count;
            scale = lerp(0.1f, 1.0f, scale * scale);
            vec.mul(scale);
            kernel[i] = vec;
        }
    }

    public void blit() {
        fbo.blitToScreen();
    }


    private static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    private int createRotationTexture() {
        Random rnd = new Random();
        int size = 4;
        int textureSize = size * size;
        float[] textureContent = new float[textureSize * 3];
        for (int i = 0; i < textureSize; i++) {
            textureContent[i * 3] = rnd.nextFloat();
            textureContent[i * 3 + 1] = rnd.nextFloat();
            textureContent[i * 3 + 2] =  rnd.nextFloat();
        }
        return TextureLoader.generateTexture(size, size, textureContent, GL40.GL_REPEAT, GL30.GL_NEAREST, GL11.GL_FLOAT, GL40.GL_RGB8, GL20.GL_RGB);
    }
}
