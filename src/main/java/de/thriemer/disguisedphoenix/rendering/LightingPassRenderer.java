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
        gResolveFactory.withUniforms("depthTexture", "normalAndSpecularTexture", "colorAndGeometryCheckTexture", "ambientOcclusionTexture","shadowMapTexture", "projMatrixInv", "lightPos", "lightColor", "ssaoEnabled", "shadowsEnabled");
        gResolveFactory.withUniformArray("shadowReprojectionMatrix", 4);
        gResolveFactory.configureSampler("depthTexture", 0).configureSampler("normalAndSpecularTexture", 1).
                configureSampler("colorAndGeometryCheckTexture", 2).configureSampler("ambientOcclusionTexture", 3).configureSampler("shadowMapTexture", 4);
        shader = gResolveFactory.built();
        deferredResult= new FrameBufferObject(width, height, 2).addTextureAttachment(0).addTextureAttachment(1).unbind();
    }

    public void render(FrameBufferObject gBuffer, ShadowRenderer shadowRenderer, CameraInformation cameraInformation, Vector3f lightPos, Vector3f lightColor) {
        lightTimer.startQuery();
        deferredResult.bind();
        shader.bind();
        bindTextures(shader,gBuffer, shadowRenderer.ssaoEffect.getSSAOTexture(), shadowRenderer.getShadowTexture());

        shader.loadInt("ssaoEnabled", shadowRenderer.ssaoEffect.isEnabled() ? 1 : 0);
        shader.loadInt("shadowsEnabled", shadowRenderer.shadowEffect.isEnabled() ? 1 : 0);
        shader.load3DVector("lightPos", cameraInformation.getViewMatrix().transformPosition(new Vector3f(lightPos)));
        shader.load3DVector("lightColor", lightColor);
        shader.loadMatrix("projMatrixInv", cameraInformation.getInvertedProjectionMatrix());
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


    private void bindTextures(Shader shader,FrameBufferObject gBuffer, int ssaoTexture, int shadowTexture) {
        shader.bind2DTexture("depthTexture",gBuffer.getDepthTexture());
        shader.bind2DTexture("normalAndSpecularTexture",gBuffer.getTextureID(0));
        shader.bind2DTexture("colorAndGeometryCheckTexture",gBuffer.getTextureID(1));
        shader.bind2DTexture("ambientOcclusionTexture",ssaoTexture);
        shader.bind2DTexture("shadowMapTexture",shadowTexture);
    }

}
