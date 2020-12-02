package graphics.postProcessing;

import disuguisedPhoenix.Main;
import graphics.objects.Shader;
import graphics.objects.Vao;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class QuadRenderer {

    Vao quad;
    Shader shader;
    Shader testShader;

    public QuadRenderer() {
        quad = new Vao();
        quad.addDataAttributes(0, 3, new float[]{-1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
        });
        quad.unbind();
        shader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/deferred/lightingPassFS.glsl")).combine("pos");
        shader.loadUniforms("depthTexture", "normalAndSpecularTexture", "colorAndGeometryCheckTexture", "ambientOcclusionTexture","projMatrixInv","lightPos","ssaoEnabled");
        shader.connectSampler("depthTexture", 0);
        shader.connectSampler("normalAndSpecularTexture", 1);
        shader.connectSampler("colorAndGeometryCheckTexture", 2);
        shader.connectSampler("ambientOcclusionTexture", 3);

        shader.unbind();

        testShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("textureTestFS.glsl")).combine("pos");
        testShader.loadUniforms("toTest");
        testShader.connectSampler("toTest", 0);
        testShader.unbind();

    }


    public void renderDeferredLightingPass(Matrix4f viewMatrix,Matrix4f projMatrix,Vector3f lightPos,boolean ssaoIsEnabled) {
        shader.bind();
        shader.loadInt("ssaoEnabled",ssaoIsEnabled?1:0);
        shader.load3DVector("lightPos", viewMatrix.transformPosition(new Vector3f(lightPos)));
        shader.loadMatrix("projMatrixInv",new Matrix4f(projMatrix).invert());
        renderOnlyQuad();
        shader.unbind();
    }

    public void renderTextureToScreen(int texture) {
        testShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, texture);
        renderOnlyQuad();
        testShader.unbind();
    }

    public void renderOnlyQuad() {
        quad.bind();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        Main.drawCalls++;
        quad.unbind();
    }

}
