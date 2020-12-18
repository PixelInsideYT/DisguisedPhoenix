package graphics.postProcessing;

import engine.util.ModelFileHandler;
import graphics.loader.MeshInformation;
import graphics.objects.FrameBufferObject;
import graphics.objects.OpenGLState;
import graphics.objects.Shader;
import graphics.objects.Vao;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL40;

public class GodRays {

    private final Shader lightShader;
    private final Shader radialBlur;
    private final Vao lightShape;
    private float rotation = 0f;
    private final Matrix4f matrix;
    private final QuadRenderer renderer;
    int downsample = 2;
    private final FrameBufferObject lightRenderd;
    private final FrameBufferObject result;

    public GodRays(QuadRenderer renderer) {
        this.renderer = renderer;
        matrix = new Matrix4f();
        //load light shape
        MeshInformation information = ModelFileHandler.loadModelToMeshInfo("misc/lightPentagon.modelFile");
        lightShape = new Vao();
        int vertexCount = information.vertexPositions.length / 4;
        float[] onlyVertexPos = new float[vertexCount * 3];
        for (int v = 0; v < vertexCount; v++) {
            onlyVertexPos[v * 3] = information.vertexPositions[v * 4];
            onlyVertexPos[v * 3 + 1] = information.vertexPositions[v * 4 + 1];
            onlyVertexPos[v * 3 + 2] = information.vertexPositions[v * 4 + 2];
        }
        lightRenderd = new FrameBufferObject(1920/downsample, 1080/downsample, 1).addTextureAttachment(0);
        result = new FrameBufferObject(1920/downsample, 1080/downsample, 1).addTextureAttachment(0);
        lightShape.addDataAttributes(0, 3, onlyVertexPos);
        lightShape.addIndicies(information.indicies);
        lightShape.unbind();
        //setup the light render shader

        lightShader = new Shader(Shader.loadShaderCode("postProcessing/godRays/lightSourceVS.glsl"), Shader.loadShaderCode("postProcessing/godRays/lightSourceFS.glsl")).combine("pos");
        lightShader.loadUniforms("geometryCheckTexture", "color", "matrix");
        lightShader.connectSampler("geometryCheckTexture", 0);
        lightShader.unbind();

        radialBlur = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/godRays/radialBlurFS.glsl")).combine("pos");
        radialBlur.loadUniforms("blurCenter", "image");

    }

    public int getTexture(){
        return result.getTextureID(0);
    }

    public void render(Matrix4f projMatrix, Matrix4f viewMatrix, Vector3f lightPos, int geometryInfoTexture) {
        //get light source position
        Vector4f clipPos = new Matrix4f(projMatrix).mul(viewMatrix).transform(new Vector4f(lightPos, 1.0f));
        Vector3f screenPos = new Vector3f(clipPos.x / clipPos.w, clipPos.y / clipPos.w, 0f);
        Vector2f uvPos = new Vector2f(screenPos.x * 0.5f + 0.5f, screenPos.y * 0.5f + 0.5f);
        rotation += 0.01f;
        float aspectRatio = (16f / 9f);
        float scale = 0.1f;
        matrix.identity();
        matrix.translate(screenPos);
        matrix.scale(scale / aspectRatio, scale, 1f);
        matrix.rotateZ(rotation);
        //render light source
        OpenGLState.disableBackFaceCulling();
        lightRenderd.bind();
        lightRenderd.clear(0f,0f,0f,0f);
        lightShape.bind();
        lightShader.bind();
        lightShader.load3DVector("color", new Vector3f(10, 10, 10));
        lightShader.loadMatrix("matrix", matrix);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, geometryInfoTexture);
        GL40.glDrawElements(GL11.GL_TRIANGLES, lightShape.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
        lightShader.unbind();
        lightShape.unbind();
        lightRenderd.unbind();
        result.bind();
        result.clear(0,0,0,0);
        radialBlur.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, lightRenderd.getTextureID(0));
        radialBlur.load2DVector("blurCenter", uvPos);
        renderer.renderOnlyQuad();
        radialBlur.unbind();
        result.unbind();
    }
}
