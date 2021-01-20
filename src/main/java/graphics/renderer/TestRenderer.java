package graphics.renderer;

import disuguisedPhoenix.Entity;
import disuguisedPhoenix.Main;
import graphics.shaders.Shader;
import graphics.objects.Vao;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;

import java.util.List;
import java.util.Map;


public class TestRenderer {

    private final Shader shader;

    public TestRenderer(Shader shader) {
        this.shader = shader;
    }

    public void begin(Matrix4f camMatrix, Matrix4f projMatrix) {
        shader.bind();
        shader.loadMatrix("projMatrix", projMatrix);
        shader.loadMatrix("viewMatrix", camMatrix);
    }

    public void render(Model model, Matrix4f... modelMatrixArray) {
        model.renderInfo.actualVao.bind();
        int indiciesLength = model.renderInfo.actualVao.getIndiciesLength();
        for (Matrix4f modelMatrix : modelMatrixArray) {
            shader.loadMatrix("transformationMatrixUniform", modelMatrix);
            GL40.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, model.renderInfo.indexOffset * 4, model.renderInfo.vertexOffset);
            Main.inViewObjects++;
            Main.inViewVerticies+=indiciesLength;
            Main.drawCalls++;
            Main.facesDrawn += indiciesLength / 3;
        }
        model.renderInfo.actualVao.unbind();
    }

    public void render(Map<Model, List<Entity>> toRender) {
        for (Model model : toRender.keySet()) {
            Vao mesh = model.renderInfo.actualVao;
            mesh.bind();
            int indiciesLength = mesh.getIndiciesLength();
            for (Entity e : toRender.get(model)) {
                shader.loadMatrix("transformationMatrixUniform", e.getTransformationMatrix());
                GL40.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, model.renderInfo.indexOffset * 4, model.renderInfo.vertexOffset);
                Main.inViewObjects++;
                Main.inViewVerticies+=indiciesLength;
                Main.drawCalls++;
                Main.facesDrawn += indiciesLength / 3;
            }
            mesh.unbind();
        }
    }

}
