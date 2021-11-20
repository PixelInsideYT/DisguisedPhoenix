package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.objects.GPUTimerQuery;
import de.thriemer.graphics.core.objects.OpenGLState;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import de.thriemer.graphics.occlusion.ShadowEffect;
import de.thriemer.graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;

public class LightingPassRenderer {

    GPUTimerQuery lightTimer = new GPUTimerQuery("Lighting Pass");
    private final QuadRenderer quadRenderer;
    FrameBufferObject deferredResult;
    private final Shader shader;

    public LightingPassRenderer(QuadRenderer quadRenderer, int width, int height) {
        this.quadRenderer = quadRenderer;
        ShaderFactory gResolveFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/deferred/lightingPassFS.glsl").withAttributes("pos");
        gResolveFactory.withUniforms("depthTexture", "splitRange", "shadowMapTexture", "zFar", "normalAndSpecularTexture", "colorAndGeometryCheckTexture", "ambientOcclusionTexture", "projMatrixInv", "lightPos", "lightColor", "ssaoEnabled", "shadowsEnabled");
        gResolveFactory.withUniformArray("shadowReprojectionMatrix", 4);
        gResolveFactory.configureSampler("depthTexture", 0).configureSampler("normalAndSpecularTexture", 1).
                configureSampler("colorAndGeometryCheckTexture", 2).configureSampler("ambientOcclusionTexture", 3).configureSampler("shadowMapTexture", 4);
        shader = gResolveFactory.built();
        deferredResult= new FrameBufferObject(width, height, 2).addTextureAttachment(0).addTextureAttachment(1).unbind();
    }

    public void render(FrameBufferObject gBuffer, ShadowRenderer shadowRenderer, CameraInformation cameraInformation, Matrix4f viewMatrix, Vector3f lightPos, Vector3f lightColor) {
        lightTimer.startQuery();
        deferredResult.bind();
        bindTextures(gBuffer, shadowRenderer.ssaoEffect.getSSAOTexture(), shadowRenderer.shadowEffect.getShadowTextureArray());
        shader.bind();
        shader.loadInt("ssaoEnabled", shadowRenderer.ssaoEffect.isEnabled() ? 1 : 0);
        shader.loadInt("shadowsEnabled", shadowRenderer.shadowEffect.isEnabled() ? 1 : 0);
        shader.load3DVector("lightPos", viewMatrix.transformPosition(new Vector3f(lightPos)));
        shader.load3DVector("lightColor", lightColor);
        shader.loadFloat("zFar", cameraInformation.getFarPlane());
        shader.loadMatrix("projMatrixInv", cameraInformation.getInvertedProjectionMatrix());
        shader.loadFloatArray("splitRange", ShadowEffect.CASCADE_DISTANCE);
        Matrix4f[] shadowReprojected = shadowRenderer.shadowEffect.getShadowProjViewMatrix();
        Matrix4f[] shadowReprojectionMatrix = new Matrix4f[shadowReprojected.length];
        for (int i = 0; i < shadowReprojectionMatrix.length; i++) {
            shadowReprojectionMatrix[i] = new Matrix4f(shadowReprojected[i]).mul(new Matrix4f(viewMatrix).invert());
        }
        shader.loadMatrix4fArray("shadowReprojectionMatrix", shadowReprojectionMatrix);
        quadRenderer.renderOnlyQuad();
        shader.unbind();
        OpenGLState.enableDepthTest();
        OpenGLState.enableAlphaBlending();
        //TODO: particle rendering
        OpenGLState.disableAlphaBlending();
        OpenGLState.disableDepthTest();
        deferredResult.unbind();
        lightTimer.stopQuery();
    }


    private void bindTextures(FrameBufferObject gBuffer, int ssaoTexture, int shadowTextureArray) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getDepthTexture());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getTextureID(0));
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getTextureID(1));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, ssaoTexture);
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowTextureArray);
    }

}
