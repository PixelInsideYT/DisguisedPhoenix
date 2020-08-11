package graphics.renderer;

import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.world.Material;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;


public class TestRenderer {

    private Shader shader;
    private Material currentMaterial;

    public TestRenderer(Shader shader) {
        this.shader = shader;
    }

    public void begin(Matrix4f camMatrix, Matrix4f projMatrix) {
        shader.bind();
        shader.loadMatrix("projMatrix", projMatrix);
        shader.loadMatrix("viewMatrix", camMatrix);
    }

    public void render(Model model, Matrix4f... modelMatrixArray) {
        for (Vao toRender : model.meshes) {
            toRender.bind();
            for (Matrix4f modelMatrix : modelMatrixArray) {
                shader.loadMatrix("transformationMatrix", modelMatrix);
                GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
            }
            toRender.unbind();
        }
    }

    public void end() {
        shader.unbind();
    }

}
