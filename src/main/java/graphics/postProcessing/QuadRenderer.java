package graphics.postProcessing;

import graphics.objects.Shader;
import graphics.objects.Vao;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class QuadRenderer {

    Vao quad;
    Shader shader;

    public QuadRenderer() {
        quad = new Vao("post process quad");
        quad.addDataAttributes(0, 3, new float[]{-1.0f, -1.0f, 0.0f,
                1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, -1.0f, 0.0f,
                1.0f, 1.0f, 0.0f,
                -1.0f, 1.0f, 0.0f,
        });
        quad.unbind();
        shader = new Shader(Shader.loadShaderCode("TextureBlitVS.glsl"),Shader.loadShaderCode("TextureBlitFS.glsl")).combine("pos");
        shader.loadUniforms("positionTexture","normalTexture","colorAndSpecularTexture","cameraPos");
        shader.connectSampler("positionTexture",0);
        shader.connectSampler("normalTexture",1);
        shader.connectSampler("colorAndSpecularTexture",2);
        shader.unbind();
    }


    public void render(Vector3f camPos){
        shader.bind();
        shader.load3DVector("cameraPos",camPos);
        quad.bind();
        GL11.glDrawArrays(GL11.GL_TRIANGLES,0,6);
        quad.unbind();
        shader.unbind();
    }

}
