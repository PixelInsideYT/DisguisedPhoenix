package graphics.occlusion;

import graphics.objects.FrameBufferObject;
import graphics.renderer.MultiIndirectRenderer;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ShadowEffect {

    private ShadowCascade singleCascade;
    private Shader shadowShader;

    public ShadowEffect() {
        singleCascade = new ShadowCascade();
        ShaderFactory shaderFactory = new ShaderFactory("shadows/shadowVSMultiDraw.glsl", "shadows/shadowFS.glsl");
        shaderFactory.withAttributes("posAndWobble", "colorAndShininess", "transformationMatrix");
        shaderFactory.withUniforms("viewProjMatrix","noiseMap","time");
        shaderFactory.configureSampler("noiseMap",0);
        shadowShader= shaderFactory.built();
    }

    public void render(Matrix4f viewMatrix, float fov, float aspect,float time, Vector3f lightPos, MultiIndirectRenderer renderer) {
        singleCascade.update(viewMatrix, 1f, fov, aspect, lightPos);
        shadowShader.bind();
        shadowShader.loadFloat("time",time);
        shadowShader.loadMatrix("viewProjMatrix", singleCascade.getViewProjMatrix());
        singleCascade.shadowMap.bind();
        singleCascade.shadowMap.clear();
        renderer.render();
        singleCascade.shadowMap.unbind();
        shadowShader.unbind();
    }


    public int getShadowTexture() {
        return singleCascade.getShadowTexture();
    }



    public Matrix4f getShadowProjViewMatrix() {
        return singleCascade.getShadowProjViewMatrix();
    }
}
