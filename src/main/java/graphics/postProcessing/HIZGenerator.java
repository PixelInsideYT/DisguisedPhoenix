package graphics.postProcessing;

import graphics.objects.FrameBufferObject;
import graphics.objects.OpenGLState;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;

public class HIZGenerator {

    private final Shader hizShader;
    private QuadRenderer renderer;
    public HIZGenerator(QuadRenderer renderer){
        ShaderFactory hiZFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/hiZGen/HI-Z-generator.glsl").withAttributes("pos");
        hiZFactory.withUniforms("LastMip", "LastMipSize");
        hiZFactory.configureSampler("LastMip", 0);
        hizShader = hiZFactory.built();
        this.renderer=renderer;
    }

    public void generateHiZMipMap(FrameBufferObject fbo) {
        fbo.bind();
        int numLevels =1+ (int) Math.floor(Math.log(Math.max(fbo.getBufferWidth(), fbo.getBufferHeight())) / Math.log(2));
        int currentWidth = fbo.getBufferWidth();
        int currentHeight = fbo.getBufferHeight();
        OpenGLState.enableDepthTest();
        int depthTexture=fbo.getDepthTexture();
        GL13.glDepthFunc(GL13.GL_ALWAYS);
        GL30.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        hizShader.bind();
        for (int i = 1; i < numLevels; i++) {
            Vector2i lastMipSize = new Vector2i(currentWidth, currentHeight);
            // calculate next viewport size
            currentWidth /= 2;
            currentHeight /= 2;
            // ensure that the viewport size is always at least 1x1
            currentWidth = currentWidth > 0 ? currentWidth : 1;
            currentHeight = currentHeight > 0 ? currentHeight : 1;
            GL13.glViewport(0, 0, currentWidth, currentHeight);
            // bind next level for rendering but first restrict fetches only to previous level
            GL13.glTexParameteri(GL11.GL_TEXTURE_2D, GL13.GL_TEXTURE_BASE_LEVEL, i - 1);
            GL13.glTexParameteri(GL13.GL_TEXTURE_2D, GL13.GL_TEXTURE_MAX_LEVEL, i - 1);
            GL43.glFramebufferTexture2D(GL43.GL_FRAMEBUFFER, GL43.GL_DEPTH_ATTACHMENT, GL43.GL_TEXTURE_2D, depthTexture, i);
            hizShader.loadIVec2("LastMipSize", lastMipSize);
            renderer.renderOnlyQuad();
        }
// reset mipmap level range for the depth image
        GL43.glTexParameteri(GL43.GL_TEXTURE_2D, GL43.GL_TEXTURE_BASE_LEVEL, 0);
        GL43.glTexParameteri(GL43.GL_TEXTURE_2D, GL43.GL_TEXTURE_MAX_LEVEL, numLevels - 1);
        GL43.glFramebufferTexture2D(GL43.GL_FRAMEBUFFER, GL43.GL_DEPTH_ATTACHMENT, GL43.GL_TEXTURE_2D, depthTexture, 0);
        GL13.glDepthFunc(GL13.GL_LEQUAL);
        hizShader.unbind();
    }

}
