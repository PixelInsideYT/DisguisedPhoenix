package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL13;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class DepthOfField {

    private FrameBufferObject helperFbo;
    private FrameBufferObject hvBlur;
    private GaussianBlur blur;
    private QuadRenderer renderer;

    private Matrix4f projMatrixInv;
    private float farPlane = 10000;

    private Shader depthOfFieldShader;

    public DepthOfField(QuadRenderer renderer, GaussianBlur blur, Matrix4f projMatrix) {
        this.renderer = renderer;
        this.blur = blur;
        depthOfFieldShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/depthOfFieldFS.glsl")).combine("pos");
        depthOfFieldShader.loadUniforms("color", "bluredColor", "depthTexture", "projMatrixInv", "focusPoint", "focusRange");
        depthOfFieldShader.bind();
        this.projMatrixInv = new Matrix4f(projMatrix).invert();
        depthOfFieldShader.loadMatrix("projMatrixInv", projMatrixInv);
        depthOfFieldShader.connectSampler("color", 0);
        depthOfFieldShader.connectSampler("bluredColor", 1);
        depthOfFieldShader.connectSampler("depthTexture", 2);
        helperFbo = new FrameBufferObject(1920, 1080, 1).addTextureAttachment(0);
        hvBlur = new FrameBufferObject(1920, 1080, 1).addTextureAttachment(0);
    }

    public void render(int color, int depth) {
        blur.blur(color, 1920, 1080, helperFbo, hvBlur, 1);
        helperFbo.bind();
        depthOfFieldShader.bind();
        computeCameraParams(depth);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, color);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, hvBlur.getTextureID(0));
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, depth);
        renderer.renderOnlyQuad();
        depthOfFieldShader.unbind();
        helperFbo.unbind();
    }

    public int getTexture() {
        return helperFbo.getTextureID(0);
    }

    private void computeCameraParams(int depth) {
        //get depth texture
        int mipLevel = 5;
        int width = 1920 >> mipLevel;
        int height = 1080 >> mipLevel;
        float[] depthTexture = new float[width * height];
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, depth);
        GL13.glGetTexImage(GL13.GL_TEXTURE_2D, mipLevel, GL13.GL_DEPTH_COMPONENT, GL13.GL_FLOAT, depthTexture);
        //convert to linear depth
        //TODO: make linear depth conversion faster
     //   BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                depthTexture[y * width + x] = -toLinearDepth(new Vector2f(x / (float) width, y / (float) height), depthTexture[y * width + x]);
            //    img.setRGB(x, y, (int) (depthTexture[y * width + x] / 1000.0f * 255f));
            }
        }

        //set shader variables
        float focusPoint = depthTexture[height / 2 * width + width / 2];
        int diff = 1;
        int xDiff =0;
        int yDiff =0;
        while (focusPoint >= farPlane * 0.99f && diff < height / 2) {
            boolean breakFree = false;
            for (int y = -diff; y < diff; y++) {
                for (int x = 0; x < diff; x++) {
                    focusPoint = depthTexture[(y + height / 2) * width + (width / 2 + x)];
                    if(focusPoint<farPlane*0.99f){
                        xDiff=x;
                        yDiff=y;
                        breakFree=true;
                        break;
                    }
                }
                if(breakFree)break;
            }
            if(breakFree)break;
            diff++;
        }
        int rgb = 255;
        rgb = (rgb << 8) + 255;
        rgb = (rgb << 8) + 255;
     //   img.setRGB(width / 2+xDiff, height/2+yDiff, rgb);
        try {
        //   ImageIO.write(img, "PNG", new File("depth.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        float focusRange = Math.max(100, focusPoint / 3);
        depthOfFieldShader.loadFloat("focusPoint", focusPoint);
        depthOfFieldShader.loadFloat("focusRange", focusRange);
    }

    private float toLinearDepth(Vector2f TexCoord, float depth) {
        float z = depth * 2.0f - 1.0f;
        Vector4f clipSpacePosition = new Vector4f(TexCoord.x * 2.0f - 1.0f, TexCoord.y * 2f - 1f, z, 1.0f);
        Vector4f viewSpacePosition = projMatrixInv.transform(clipSpacePosition);
        viewSpacePosition.mul(1f / viewSpacePosition.w);
        return viewSpacePosition.z;
    }

}
