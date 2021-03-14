package graphics.postprocessing;

import graphics.objects.FrameBufferObject;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;

import static org.lwjgl.opengl.GL13.*;

public class Combine {

    private final Shader shader;
    private final QuadRenderer renderer;
    private final FrameBufferObject combinedResult;

    public Combine(int width, int height,QuadRenderer renderer){
        this.renderer=renderer;
        ShaderFactory shaderFactory = new ShaderFactory("postProcessing/quadVS.glsl","postProcessing/combine/combineFS.glsl").withAttributes("pos");
        shaderFactory.withUniforms("colorTexture","bloomTexture","godRaysTexture");
        shaderFactory.configureSampler("colorTexture",0).configureSampler("bloomTexture",1).configureSampler("godRaysTexture",2);
        shader = shaderFactory.built();
        combinedResult = new FrameBufferObject(width, height, 1).addTextureAttachment(0).unbind();
    }

    public int getCombinedResult(){
        return combinedResult.getTextureID(0);
    }

    public void render(int lightingTexture, int bloomTexture){
        combinedResult.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D,lightingTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D,bloomTexture);
       // GL13.glActiveTexture(GL13.GL_TEXTURE2);
       // GL13.glBindTexture(GL13.GL_TEXTURE_2D,godRaysTexture);
        shader.bind();
        renderer.renderOnlyQuad();
        shader.unbind();
        combinedResult.unbind();
    }

    public void resize(int width, int height) {
        combinedResult.resize(width, height);
    }
}
