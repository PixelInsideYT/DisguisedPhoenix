package de.thriemer.graphics.postprocessing;

import de.thriemer.graphics.core.objects.GPUTimerQuery;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;

import static org.lwjgl.opengl.GL13.*;

public class FXAARenderer {

    private final Shader aliasShader;
    private final QuadRenderer renderer;
    private final GPUTimerQuery fxaaTimer=new GPUTimerQuery("FXAA");

    public FXAARenderer(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory aliasFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/antiAliasing/FXAA.glsl").withAttributes("pos");
        aliasFactory.withUniforms("tex").configureSampler("tex", 0);
        aliasShader = aliasFactory.built();
    }


    public void render(int colorTexture) {
        fxaaTimer.startQuery();
        aliasShader.bind();
        aliasShader.bind2DTexture("tex",colorTexture);
        renderer.renderOnlyQuad();
        aliasShader.unbind();
        fxaaTimer.stopQuery();
    }

}
