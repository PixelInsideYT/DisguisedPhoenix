package de.thriemer.graphics.postprocessing;

import de.thriemer.disguisedphoenix.Main;
import de.thriemer.graphics.core.objects.Vao;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL13.*;

public class QuadRenderer {

    Vao quad;
    Shader testShader;

    public QuadRenderer() {
        quad = new Vao();
        quad.addDataAttributes(0, 3, new float[]{-1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
        });
        quad.unbind();
        testShader = new ShaderFactory("postProcessing/quadVS.glsl", "textureTestFS.glsl")
                .withAttributes("pos").withUniforms("toTest").configureSampler("toTest", 0).built();
    }

    public void renderTextureToScreen(int texture) {
        testShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        renderOnlyQuad();
        testShader.unbind();
    }

    public void renderOnlyQuad() {
        quad.bind();
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        Main.drawCalls++;
        quad.unbind();
    }

}
