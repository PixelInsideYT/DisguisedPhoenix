package graphics.postprocessing;

import graphics.core.objects.FrameBufferObject;
import graphics.core.objects.OpenGLState;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;
import org.joml.Vector2i;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL43.*;

public class HIZGenerator {

    private final Shader hizShader;
    private final QuadRenderer renderer;

    public HIZGenerator(QuadRenderer renderer) {
        ShaderFactory hiZFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/hiZGen/HI-Z-generator.glsl").withAttributes("pos");
        hiZFactory.withUniforms("LastMip", "LastMipSize");
        hiZFactory.configureSampler("LastMip", 0);
        hizShader = hiZFactory.built();
        this.renderer = renderer;
    }

    public void generateHiZMipMap(FrameBufferObject fbo) {
        fbo.bind();
        int numLevels = 1 + (int) Math.floor(Math.log(Math.max(fbo.getBufferWidth(), fbo.getBufferHeight())) / Math.log(2));
        int currentWidth = fbo.getBufferWidth();
        int currentHeight = fbo.getBufferHeight();
        OpenGLState.enableDepthTest();
        int depthTexture = fbo.getDepthTexture();
        glDepthFunc(GL_ALWAYS);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        hizShader.bind();
        for (int i = 1; i < numLevels; i++) {
            Vector2i lastMipSize = new Vector2i(currentWidth, currentHeight);
            // calculate next viewport size
            currentWidth /= 2;
            currentHeight /= 2;
            // ensure that the viewport size is always at least 1x1
            currentWidth = currentWidth > 0 ? currentWidth : 1;
            currentHeight = currentHeight > 0 ? currentHeight : 1;
            glViewport(0, 0, currentWidth, currentHeight);
            // bind next level for rendering but first restrict fetches only to previous level
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, i - 1);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, i - 1);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, i);
            hizShader.loadIVec2("LastMipSize", lastMipSize);
            renderer.renderOnlyQuad();
        }
// reset mipmap level range for the depth image
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, numLevels - 1);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        glDepthFunc(GL_LEQUAL);
        hizShader.unbind();
    }

}
