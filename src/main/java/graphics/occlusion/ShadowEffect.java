package graphics.occlusion;

import disuguisedphoenix.Entity;
import graphics.core.objects.FrameBufferObject;
import graphics.core.objects.GPUTimerQuery;
import graphics.core.renderer.MultiIndirectRenderer;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL42.glTexStorage3D;

public class ShadowEffect {

    public static final float[] CASCADE_DISTANCE = {0.01f, 0.05f, 0.25f, 1f};
    private static final int SHADOW_RESOLUTION = 2048;
    private static final int SHADOWS_CASCADES = 4;
    private GPUTimerQuery shadowTimer;
    protected FrameBufferObject[] shadowMap = new FrameBufferObject[SHADOWS_CASCADES];
    int textureArray;
    private final ShadowCascade[] cascades = new ShadowCascade[SHADOWS_CASCADES];
    private final Shader shadowShader;

    private boolean enabled = true;
    private final FrustumIntersection frustumIntersection = new FrustumIntersection();

    public ShadowEffect() {
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

    //TODO: create a viewport info POJO
    public void render(Matrix4f viewMatrix, float nearPlane, float farPlane, float fov, float aspect, float time, Vector3f lightPos, MultiIndirectRenderer renderer) {
        if (isEnabled()) {
            List<List<Entity>> inCascade = new ArrayList<>();
            float near = nearPlane;
            for (int i = 0; i < SHADOWS_CASCADES; i++) {
                float cascadeFar = CASCADE_DISTANCE[i] * farPlane;
                cascades[i].update(viewMatrix, near, cascadeFar, fov, aspect, lightPos);
                frustumIntersection.set(cascades[i].getViewProjMatrix(), true);
                near = cascadeFar;
                inCascade.add(renderer.currentEntities.stream().filter(e -> frustumIntersection.testSphere(e.getCenter(),e.getRadius())).collect(Collectors.toList()));
            }
            shadowTimer.startQuery();
            shadowShader.bind();
            shadowShader.loadFloat("time", time);
            for (int i = 0; i < SHADOWS_CASCADES; i++) {
                shadowMap[i].bind();
                shadowMap[i].clear();
                shadowShader.loadMatrix("viewProjMatrix", cascades[i].getViewProjMatrix());
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

    public void print() {
        if (isEnabled()) {
            shadowTimer.printResults();
        }
    }

}
