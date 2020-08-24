package graphics.renderer;

import disuguisedPhoenix.Main;
import graphics.objects.Shader;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;


public class TestRenderer {

    private Shader shader;

    public TestRenderer(Shader shader) {
        this.shader = shader;
    }

    public void begin(Matrix4f camMatrix, Matrix4f projMatrix) {
        shader.bind();
        shader.loadMatrix("projMatrix", projMatrix);
        shader.loadMatrix("viewMatrix", camMatrix);
    }

    public void render(Model model, Matrix4f... modelMatrixArray) {
        model.mesh.bind();
        int indiciesLength = model.mesh.getIndiciesLength();
        for (Matrix4f modelMatrix : modelMatrixArray) {
            shader.loadMatrix("transformationMatrix", modelMatrix);
            GL11.glDrawElements(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, 0);
            Main.drawCalls++;
        }
        model.mesh.unbind();
    }

    public void end() {
        shader.unbind();
    }

}
