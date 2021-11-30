package de.thriemer.graphics.postprocessing;

import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;

import static org.lwjgl.opengl.GL13.*;

public class Combine {

    private final Shader shader;
    private final QuadRenderer renderer;
    private final FrameBufferObject combinedResult;

    public Combine(int width, int height, QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory shaderFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/combine/combineFS.glsl").withAttributes("pos");
        shaderFactory.withUniforms("colorTexture", "bloomTexture", "godRaysTexture");
        shaderFactory.configureSampler("colorTexture", 0).configureSampler("bloomTexture", 1).configureSampler("godRaysTexture", 2);
        shader = shaderFactory.built();
        combinedResult = new FrameBufferObject(width, height, 1).addTextureAttachment(0).unbind();
    }

    public int getCombinedResult() {
        return combinedResult.getTextureID(0);
    }

    public void render(int lightingTexture, int bloomTexture) {
        combinedResult.bind();
        shader.bind();
        shader.bind2DTexture("colorTexture",lightingTexture);
        shader.bind2DTexture("bloomTexture",bloomTexture);
        renderer.renderOnlyQuad();
        shader.unbind();
        combinedResult.unbind();
    }

    public void resize(int width, int height) {
        combinedResult.resize(width, height);
    }
}
