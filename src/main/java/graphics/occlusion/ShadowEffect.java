package graphics.occlusion;

import disuguisedphoenix.Entity;
import engine.util.Maths;
import graphics.objects.FrameBufferObject;
import graphics.objects.TimerQuery;
import graphics.renderer.MultiIndirectRenderer;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
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

    private static final int SHADOW_RESOLUTION = 2048;
    private static final int SHADOWS_CASCADES = 4;
    private static final float[] CASCADE_DISTANCE = {0.02f, 0.05f, 0.5f, 1f};

    int textureArray;
    protected FrameBufferObject[] shadowMap = new FrameBufferObject[SHADOWS_CASCADES];
    private ShadowCascade[] cascades = new ShadowCascade[SHADOWS_CASCADES];
    public TimerQuery shadowTimer;
    private Shader shadowShader;

    private boolean enabled = true;

    public ShadowEffect() {
        generate2DTextureArray();
        shadowTimer = new TimerQuery("Cascading Shadows");
        for (int i = 0; i < SHADOWS_CASCADES; i++) {
            cascades[i] = new ShadowCascade();
            shadowMap[i] = new FrameBufferObject(SHADOW_RESOLUTION, SHADOW_RESOLUTION, 0).addLayeredDepthTextureAttachment(textureArray, i);
        }
        ShaderFactory shaderFactory = new ShaderFactory("shadows/shadowVSMultiDraw.glsl", "shadows/shadowFS.glsl");//.addShaderStage(ShaderFactory.GEOMETRY_SHADER,"shadows/shadowGS.glsl");
        shaderFactory.withAttributes("posAndWobble", "colorAndShininess", "transformationMatrix");
        shaderFactory.withUniforms("noiseMap", "time", "viewProjMatrix");
        shaderFactory.configureSampler("noiseMap", 0);
        shadowShader = shaderFactory.built();
    }

    private FrustumIntersection frustumIntersection = new FrustumIntersection();

    public void render(Matrix4f viewMatrix, float nearPlane, float farPlane, float fov, float aspect, float time, Vector3f lightPos, List<Entity> worldEntities, MultiIndirectRenderer renderer) {
        if (isEnabled()) {
            List<List<Entity>> inCascade = new ArrayList<>();
            float near = nearPlane;
            for (int i = 0; i < SHADOWS_CASCADES; i++) {
                float cascadeFar = CASCADE_DISTANCE[i] * farPlane;
                cascades[i].update(viewMatrix, near, cascadeFar, fov, aspect, lightPos);
                frustumIntersection.set(cascades[i].getViewProjMatrix(), true);
                near = cascadeFar;
                inCascade.add(renderer.currentEntities.stream().filter(e -> Maths.isInsideFrustum(frustumIntersection, e)).collect(Collectors.toList()));
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
            shadowTimer.waitOnQuery();
        }
    }


    public int getShadowTexture() {
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

    public void print(){
        if(isEnabled()){
            shadowTimer.printResults();
        }
    }

}
