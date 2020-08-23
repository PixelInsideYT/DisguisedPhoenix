package graphics.renderer;

import graphics.objects.Shader;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class InstancedRenderer implements IRenderer {

    private Shader shader;
    private int instanceCount;

    public InstancedRenderer(Shader instancedShader, int instanceCount) {
        this.shader = instancedShader;
        this.instanceCount=instanceCount;

    }

    @Override
    public void begin(Matrix4f camMatrix, Matrix4f projMatrix) {
        shader.bind();
        shader.loadMatrix("projMatrix", projMatrix);
        shader.loadMatrix("viewMatrix", camMatrix);
    }

    @Override
    public void render(Model model, Matrix4f... modelMatrixArray) {
        model.mesh.bind();
        int indiciesLength = model.mesh.getIndiciesLength();
        for (Matrix4f modelMatrix : modelMatrixArray) {
            shader.loadMatrix("transformationMatrix", modelMatrix);
            GL11.glDrawElements(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, 0);
        }
        model.mesh.unbind();
    }

    @Override
    public void end() {
        shader.unbind();
    }
}
