package graphics.occlusion;

import disuguisedphoenix.Main;
import graphics.objects.FrameBufferObject;
import graphics.renderer.MultiIndirectRenderer;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;

public class ShadowEffect {

    private static final int SHADOWS_CASCADES = 4;
    private static final float[] CASCADE_DISTANCE = {0.05f,0.02f,0.5f,1f};

    protected FrameBufferObject shadowMap;
    private ShadowCascade[] cascades =new ShadowCascade[SHADOWS_CASCADES];
    private Shader shadowShader;

    public ShadowEffect() {
        for(int i=0;i<SHADOWS_CASCADES;i++){
            cascades[i]=new ShadowCascade();
        }
        shadowMap = new FrameBufferObject(1024, 1024, 0).addLayeredDepthTextureAttachment(SHADOWS_CASCADES,1024);
        ShaderFactory shaderFactory = new ShaderFactory("shadows/shadowVSMultiDraw.glsl", "shadows/shadowFS.glsl").addShaderStage(ShaderFactory.GEOMETRY_SHADER,"shadows/shadowGS.glsl");
        shaderFactory.withAttributes("posAndWobble", "colorAndShininess", "transformationMatrix");
        shaderFactory.withUniforms("noiseMap","time");
        shaderFactory.withUniformArray("viewProjMatrix",4);
        shaderFactory.configureSampler("noiseMap",0);
        shadowShader= shaderFactory.built();
    }

    public void render(Matrix4f viewMatrix, float fov, float aspect,float time, Vector3f lightPos, MultiIndirectRenderer renderer) {
        float near = 1f;
        for(int i=0;i<SHADOWS_CASCADES;i++) {
            cascades[i].update(viewMatrix, near, CASCADE_DISTANCE[i]* Main.FAR_PLANE, fov, aspect, lightPos);
            near = CASCADE_DISTANCE[i]* Main.FAR_PLANE;
        }
        shadowShader.bind();
        shadowShader.loadFloat("time",time);
        shadowShader.loadMatrix4fArray("viewProjMatrix", Arrays.stream(cascades).map(ShadowCascade::getViewProjMatrix).toArray(Matrix4f[]::new));
        shadowMap.bind();
        shadowMap.clear();
        renderer.render();
        shadowMap.unbind();
        shadowShader.unbind();
    }


    public int getShadowTexture() {
        return shadowMap.getDepthTexture();
    }



    public Matrix4f[] getShadowProjViewMatrix() {
        return Arrays.stream( cascades).map(ShadowCascade::getShadowProjViewMatrix).toArray(Matrix4f[]::new);
    }
}
