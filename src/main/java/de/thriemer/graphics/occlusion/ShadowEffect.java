package de.thriemer.graphics.occlusion;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.rendering.CameraInformation;
import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.graphics.core.context.ContextInformation;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.objects.GPUTimerQuery;
import de.thriemer.graphics.core.renderer.MultiIndirectRenderer;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL42.glTexStorage3D;

public class ShadowEffect {

    public static final float[] CASCADE_DISTANCE = {0.01f,0.05f,0.25f,1f};
    public static final int SHADOWS_CASCADES = CASCADE_DISTANCE.length;
    private static final int SHADOW_RESOLUTION = 4096;
    //TODO better shadow bounds
    private GPUTimerQuery shadowTimer;
    protected FrameBufferObject[] shadowMap = new FrameBufferObject[SHADOWS_CASCADES];
    int textureArray;
    private final ShadowCascade[] cascades = new ShadowCascade[SHADOWS_CASCADES];
    private final Shader shadowShader;

    private boolean enabled = true;
    private ContextInformation contextInformation;

    public ShadowEffect(ContextInformation contextInformation) {
        this.contextInformation=contextInformation;
        generate2DTextureArray();
        shadowTimer = new GPUTimerQuery("Cascading Shadows");
        for (int i = 0; i < SHADOWS_CASCADES; i++) {
            cascades[i] = new ShadowCascade();
            shadowMap[i] = new FrameBufferObject(SHADOW_RESOLUTION, SHADOW_RESOLUTION, 0).addLayeredDepthTextureAttachment(textureArray, i);
        }
        ShaderFactory shaderFactory = new ShaderFactory("shadows/shadowVSMultiDraw.glsl", "shadows/shadowFS.glsl");
        shaderFactory.withAttributes("posAndWobble", "colorAndShininess", "transformationMatrix");
        shaderFactory.withUniforms("noiseMap", "time", "viewProjMatrix");
        shaderFactory.configureSampler("noiseMap", 0);
        shadowShader = shaderFactory.built();
    }
    //TODO: improve shadow quality by PCF or reprojection
    public void render(CameraInformation cameraInformation, float time, Vector3f lightPos, World world, MultiIndirectRenderer renderer) {
        if (isEnabled()) {

            List<List<Entity>> inCascade = new ArrayList<>();
            float near = cameraInformation.getNearPlane();
            for (int i = 0; i < SHADOWS_CASCADES; i++) {
                float cascadeFar = CASCADE_DISTANCE[i] * cameraInformation.getFarPlane();
                cascades[i].update(cameraInformation.getViewMatrix(), near, cascadeFar, cameraInformation.getFov(), contextInformation.getAspectRatio(), lightPos);
                near = cascadeFar;
                 int finalI = i;
                inCascade.add(world.getVisibleEntities(cascades[i].getProjViewMatrix(), e-> (e.getRadius()*e.getRadius())/cascades[finalI].size> 0.00001));
            }
            shadowTimer.startQuery();
            shadowShader.bind();
            shadowShader.loadFloat("time", time);
            for (int i = 0; i < SHADOWS_CASCADES; i++) {
                shadowMap[i].bind();
                shadowMap[i].clear();
                shadowShader.loadMatrix("viewProjMatrix", cascades[i].getProjViewMatrix());
                renderer.prepareRenderer(inCascade.get(i));
                renderer.render();
            }
            shadowMap[0].unbind();
            shadowShader.unbind();
            shadowTimer.stopQuery();
        }
    }


    public int getShadowTextureArray() {
        return textureArray;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Matrix4f[] getShadowProjViewMatrix() {
        return Arrays.stream(cascades).map(ShadowCascade::getShadowProjViewMatrix).toArray(Matrix4f[]::new);
    }

    private void generate2DTextureArray() {
        textureArray = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureArray);
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL_DEPTH_COMPONENT32F, SHADOW_RESOLUTION, SHADOW_RESOLUTION, SHADOWS_CASCADES);
    }

}
