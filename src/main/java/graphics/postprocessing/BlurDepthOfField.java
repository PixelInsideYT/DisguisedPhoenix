package graphics.postprocessing;

import graphics.objects.BufferObject;
import graphics.objects.FrameBufferObject;
import graphics.shaders.ComputeShader;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL45.*;

public class BlurDepthOfField {

    private static final String INV_PROJ_MAT = "projMatrixInv";

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
        ShaderFactory depthFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/DoF/blurDoFFS.glsl").withAttributes("pos");
        depthFactory.withUniforms("blured", "original", "depth", INV_PROJ_MAT);
        depthFactory.configureSampler("original", 0).configureSampler("blured", 1).configureSampler("depth", 2);
        depthOfFieldShader=depthFactory.built();
        projMatrixInv = new Matrix4f(projMatrix).invert();
        depthOfFieldShader.bind();
        depthOfFieldShader.loadMatrix(INV_PROJ_MAT, projMatrixInv);
        focusPointComputeShader = new ComputeShader(ShaderFactory.loadShaderCode("compute/textureFillerCS.glsl"));
        focusPointComputeShader.bind();
        focusPointComputeShader.loadUniforms("depth_input", "imageOut", INV_PROJ_MAT);
        focusPointComputeShader.loadMatrix4f(INV_PROJ_MAT, projMatrixInv);
        focusPointComputeShader.connectSampler("depth_input", 0);
        focusPointComputeShader.unbind();
        fbo0 = new FrameBufferObject(width, height, 1).addTextureAttachment(0).unbind();
        fbo1 = new FrameBufferObject(width, height, 1).addTextureAttachment(0).unbind();
        ssboTest = new BufferObject(new float[]{1000f, 100f}, GL_SHADER_STORAGE_BUFFER, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssboTest.getBufferID());
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
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, color);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, depth);
        renderer.renderOnlyQuad();
        depthOfFieldShader.unbind();
        fbo0.unbind();
        calculatedDepthOfField=false;
    }

    public void computeCameraParams(int depth) {
        focusPointComputeShader.bind();
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, ssboTest.getBufferID());
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, depth);
        focusPointComputeShader.loadImage(1, fbo0.getTextureID(0), GL_WRITE_ONLY, GL_RGBA16F);
        focusPointComputeShader.run(60, 60, 1);
        focusPointComputeShader.unbind();
        focusPointComputeShader.setSSBOAccesBarrier();
        calculatedDepthOfField=true;
    }

}
