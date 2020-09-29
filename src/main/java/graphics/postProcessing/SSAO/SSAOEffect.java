package graphics.postProcessing.SSAO;

import graphics.objects.FrameBufferObject;
import graphics.objects.Shader;
import graphics.postProcessing.GaussianBlur;
import graphics.postProcessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;

public class SSAOEffect {
    private int width, height;

    private FrameBufferObject fbo;
    private FrameBufferObject temporalFbo;
    private FrameBufferObject helperFbo;
    private Shader ssaoShader;
    private Shader blurShader;

    private QuadRenderer renderer;

    public SSAOEffect(QuadRenderer renderer,  int width, int height, Matrix4f projMatrix) {
        this.renderer = renderer;
        this.width = width;
        this.height = height;
        fbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        temporalFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0).addDepthTextureAttachment();
        helperFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
        fbo.unbind();
        ssaoShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/ssao/SSAOFS.glsl")).combine("pos");
        ssaoShader.loadUniforms("camera_positions", "projMatrixInv", "projScale","radius","samples","kontrast","sigma","beta","farPlane");
        ssaoShader.connectSampler("camera_positions", 0);
        ssaoShader.bind();
        ssaoShader.loadMatrix("projMatrixInv", new Matrix4f(projMatrix).invert());
        Vector4f uvProjScale = projMatrix.transform(new Vector4f(1, 0, -1, 1));
        float projScale = uvProjScale.x / uvProjScale.w;
        projScale = projScale * 0.5f + 0.5f;
        projScale *= (width+height)/2f;
        ssaoShader.loadFloat("projScale", projScale);
        ssaoShader.loadFloat("radius",10);
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

    int count =0;
    float min = Float.MAX_VALUE;
    float max = -Float.MAX_VALUE;
    float avg =0;

    public void renderEffect(FrameBufferObject gBuffer, Matrix4f viewMatrix, float dt) {
        int queryId = GL45.glGenQueries();
        GL45.glBeginQuery(GL45.GL_TIME_ELAPSED,queryId);
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
        GL45.glEndQuery(GL45.GL_TIME_ELAPSED);
        int[] done=new int[]{0};
        while(done[0]==0){
            GL45.glGetQueryObjectiv(queryId,GL45.GL_QUERY_RESULT_AVAILABLE,done);
        }
        long[] elapsedTime=new long[1];
        GL45.glGetQueryObjectui64v(queryId, GL15.GL_QUERY_RESULT,elapsedTime);
        float msTime = elapsedTime[0]/ 1000000.0f;
        min = Math.min(min,msTime);
        max = Math.max(max,msTime);
        avg+=msTime;
        count++;
        //System.out.println("SSAO took: "+msTime+" ms ("+min+", "+max+", "+avg/count);
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

}
