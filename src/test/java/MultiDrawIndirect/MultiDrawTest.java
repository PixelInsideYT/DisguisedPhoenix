package MultiDrawIndirect;

import graphics.context.Display;
import graphics.objects.BufferObject;
import graphics.objects.Vao;
import graphics.shaders.Shader;
import org.lwjgl.opengl.*;

public class MultiDrawTest {

    public static void main(String[] args) {
        Display display = new Display("MultiDraw Test", 1920, 1080);
        //setup VAO with multiple models
        Vao models = new Vao();
        models.addDataAttributes(0, 3, new float[]{     // a centered triangle

                -0.5f, -0.5f, 0f,
                0.5f, -0.5f, 0f,
                0f, 0.5f, 0f,

                //--------------

                // a quad (too big for the screen without scaling)
                -1f, -1f, 0f,
                1f, -1f, 0f,
                -1f, 1f, 0f,
                1f, 1f, 0f,

                //another triangle but diffrent
                0f, 1f, 0f,
                -0.5f, -0.5f, 0f,
                0.5f, -0.5f, 0f
        });
        models.addIndicies(new int[]{0, 1, 2, 0, 1, 2, 1, 2, 3, 0, 1, 2});
        BufferObject matrixVbo = new BufferObject(new float[]{  // identity matrix for the triangle

                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,

                // four matrices for the quads
                // that scale and translate them

                0.1f, 0f, 0f, 0f,
                0f, 0.1f, 0f, 0f,
                0f, 0f, 0.1f, 0f,
                -0.8f, -0.8f, 0f, 1f,

                0.1f, 0f, 0f, 0f,
                0f, 0.1f, 0f, 0f,
                0f, 0f, 0.1f, 0f,
                -0.8f, 0.8f, 0f, 1f,

                0.1f, 0f, 0f, 0f,
                0f, 0.1f, 0f, 0f,
                0f, 0f, 0.1f, 0f,
                0.8f, -0.8f, 0f, 1f,

                0.1f, 0f, 0f, 0f,
                0f, 0.1f, 0f, 0f,
                0f, 0f, 0.1f, 0f,
                0.8f, 0.8f, 0f, 1f}, GL20.GL_ARRAY_BUFFER, GL20.GL_STATIC_DRAW);
        models.addInstancedAttribute(matrixVbo, 1, 4, 16, 0);
        models.addInstancedAttribute(matrixVbo, 2, 4, 16, 4);
        models.addInstancedAttribute(matrixVbo, 3, 4, 16, 8);
        models.addInstancedAttribute(matrixVbo, 4, 4, 16, 12);
        models.unbind();

        //setup command buffer
        BufferObject cmdBuffer = new BufferObject(GL43.GL_DRAW_INDIRECT_BUFFER);
        cmdBuffer.bufferData(new int[]{
                3, 1, 0, 0, 0,
                6, 3, 3, 3, 1,
                3, 1, 9, 7, 4}, GL20.GL_STATIC_DRAW);
        GL11.glCullFace(GL12.GL_BACK);
        GL11.glDisable(GL11.GL_CULL_FACE);
        //setup shader
        Shader simpleShader = null;
        /*new Shader("\n" +
                "#version 150 core\n" +
                "\n" +
                "in vec3 pos;\n" +
                "in mat4 model;\n" +
                "\n" +
                "void main() {\n" +
                "\tgl_Position = model * vec4(pos, 1.f);\n" +
                "}", "#version 150 core\n" +
                "out vec4 col;void main() {col = vec4(1.f, 1.f, 1.f, 1.f);}").combine("pos", "model");
*/
        simpleShader.bind();
        models.bind();
        GLUtil.setupDebugMessageCallback();
        while (!display.shouldClose()) {
            display.pollEvents();
            display.clear();
            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, 0, 3, 0);
            display.flipBuffers();
        }
        Vao.cleanUpAllVaos();
        Shader.cleanUpAllShaders();
        display.destroy();
    }

}
