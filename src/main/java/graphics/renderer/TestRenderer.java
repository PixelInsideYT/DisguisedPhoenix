package graphics.renderer;

import disuguisedPhoenix.Entity;
import disuguisedPhoenix.Main;
import graphics.objects.Shader;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;


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
            Main.facesDrawn += indiciesLength / 3;
        }
        model.mesh.unbind();
    }

    public void render(Map<Model, List<Entity>> toRender) {
        for (Model model : toRender.keySet()) {
            model.mesh.bind();
            int indiciesLength = model.mesh.getIndiciesLength();
            for (Entity e : toRender.get(model)) {
                shader.loadMatrix("transformationMatrix", e.getTransformationMatrix());
                GL11.glDrawElements(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, 0);
                Main.drawCalls++;
                Main.facesDrawn += indiciesLength / 3;
            }
            model.mesh.unbind();
        }
    }

    public void end() {
        shader.unbind();
    }

}
