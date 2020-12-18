package graphics.postProcessing;

import graphics.objects.BufferObject;
import graphics.objects.ComputeShader;
import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL46;

public class BlurDepthOfField {

    private final QuadRenderer renderer;
    private final GaussianBlur blur;

    private final FrameBufferObject fbo0;
    private final FrameBufferObject fbo1;

    private final Shader depthOfFieldShader;

    private final Matrix4f projMatrixInv;
    private BufferObject ssboTest;
    ComputeShader focusPointComputeShader;
    private boolean calculatedDepthOfField = false;

    public BlurDepthOfField(int width, int height, Matrix4f projMatrix, QuadRenderer renderer, GaussianBlur blurHelper) {
        this.renderer = renderer;
        this.blur = blurHelper;

        depthOfFieldShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/DoF/blurDoFFS.glsl")).combine("pos");
        depthOfFieldShader.loadUniforms("blured", "original", "depth", "projMatrixInv");
        depthOfFieldShader.bind();
        projMatrixInv = new Matrix4f(projMatrix).invert();
        depthOfFieldShader.loadMatrix("projMatrixInv", projMatrixInv);
        depthOfFieldShader.connectSampler("original", 0);
        depthOfFieldShader.connectSampler("blured", 1);
        depthOfFieldShader.connectSampler("depth", 2);
        focusPointComputeShader = new ComputeShader(Shader.loadShaderCode("compute/textureFillerCS.glsl"));
        focusPointComputeShader.bind();
        focusPointComputeShader.loadUniforms("depth_input", "imageOut", "projMatrixInv");
        focusPointComputeShader.loadMatrix4f("projMatrixInv", projMatrixInv);
        focusPointComputeShader.connectSampler("depth_input", 0);
        focusPointComputeShader.unbind();
        fbo0 = new FrameBufferObject(width, height, 1).addUnclampedTexture(0).unbind();
        fbo1 = new FrameBufferObject(width, height, 1).addUnclampedTexture(0).unbind();
        ssboTest = new BufferObject(new float[]{1000f, 100f}, GL45.GL_SHADER_STORAGE_BUFFER, GL45.GL_DYNAMIC_DRAW);
        GL45.glBindBufferBase(GL45.GL_SHADER_STORAGE_BUFFER, 1, ssboTest.bufferID);
        ssboTest.unbind();
    }

    public int getResult() {
        return fbo0.getTextureID(0);
    }

    public void render(int color, int depth) {
        if (!calculatedDepthOfField) {
            System.out.println("Calculate focuspoint depth of field beforehand to improve performance");
            computeCameraParams(depth);
        }
        int textureID = color;
        for (int i = 0; i < 5; i++) {
            blur.blur(textureID, 1920, 1080, fbo0, fbo1, 1);
            textureID = fbo1.getTextureID(0);
        }
        fbo0.bind();
        depthOfFieldShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, color);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, textureID);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, depth);
        renderer.renderOnlyQuad();
        depthOfFieldShader.unbind();
        fbo0.unbind();
        calculatedDepthOfField=false;
    }

    public void computeCameraParams(int depth) {
        focusPointComputeShader.bind();
        GL45.glBindBufferBase(GL45.GL_SHADER_STORAGE_BUFFER, 1, ssboTest.bufferID);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, depth);
        focusPointComputeShader.loadImage(1, fbo0.getTextureID(0), GL46.GL_WRITE_ONLY, GL46.GL_RGBA16F);
        focusPointComputeShader.run(60, 60, 1);
        focusPointComputeShader.unbind();
        focusPointComputeShader.setSSBOAccesBarrier();
        calculatedDepthOfField=true;
    }

}
