package de.thriemer.graphics.occlusion;

import de.thriemer.disguisedphoenix.rendering.CameraInformation;
import de.thriemer.graphics.core.context.ContextInformation;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import de.thriemer.graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;

public class CSMResolver {

    private QuadRenderer quadRenderer;
    private Shader shadowResolveShader;
    private FrameBufferObject shadowFbo;

    public CSMResolver(QuadRenderer quadRenderer,ContextInformation contextInformation){
        this.quadRenderer=quadRenderer;
        ShaderFactory resolveFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/occlusion/shadowResolveFS.glsl").withAttributes("pos");
        resolveFactory.withUniforms("shadowsEnabled", "projMatrixInv", "zFar","splitRange","depthTexture","shadowMapTexture");
        resolveFactory.withUniformArray("shadowReprojectionMatrix",4);
        resolveFactory.configureSampler("depthTexture",0).configureSampler("shadowMapTexture", 1);
        shadowResolveShader = resolveFactory.built();
        shadowFbo=new FrameBufferObject(contextInformation.getWidth(),contextInformation.getHeight(),1).addTextureAttachment(0);
    }

    public void render(CameraInformation cameraInformation,int depthTexture, ShadowEffect shadowEffect){
        shadowFbo.bind();
        shadowResolveShader.bind();
        shadowResolveShader.bind2DTexture("depthTexture",depthTexture);
        shadowResolveShader.bind2DTextureArray("shadowMapTexture",shadowEffect.getShadowTextureArray());
        shadowResolveShader.loadInt("shadowsEnabled", shadowEffect.isEnabled() ? 1 : 0);
        shadowResolveShader.loadFloat("zFar", cameraInformation.getFarPlane());
        shadowResolveShader.loadMatrix("projMatrixInv", cameraInformation.getInvertedProjectionMatrix());
        shadowResolveShader.loadFloatArray("splitRange", ShadowEffect.CASCADE_DISTANCE);
        Matrix4f[] shadowReprojected = shadowEffect.getShadowProjViewMatrix();
        Matrix4f[] shadowReprojectionMatrix = new Matrix4f[shadowReprojected.length];
        Matrix4f invertedCameraMatrix = new Matrix4f(cameraInformation.getViewMatrix()).invert();
        for (int i = 0; i < shadowReprojectionMatrix.length; i++) {
            shadowReprojectionMatrix[i] = new Matrix4f(shadowReprojected[i]).mul(invertedCameraMatrix);
        }
        shadowResolveShader.loadMatrix4fArray("shadowReprojectionMatrix", shadowReprojectionMatrix);
        quadRenderer.renderOnlyQuad();
        shadowResolveShader.unbind();
        shadowFbo.unbind();
    }

    public void resize(ContextInformation contextInformation) {
        shadowFbo.resize(contextInformation.getWidth(),contextInformation.getHeight());
    }

    public int getShadowTexture() {
        return shadowFbo.getTextureID(0);
    }
}
