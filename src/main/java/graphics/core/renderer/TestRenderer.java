package graphics.core.renderer;

import disuguisedphoenix.Entity;
import disuguisedphoenix.Main;
import graphics.core.objects.Vao;
import graphics.core.shaders.Shader;
import graphics.modelinfo.Model;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL40.glDrawElementsBaseVertex;


public class TestRenderer {

    private final Shader shader;

    public TestRenderer(Shader shader) {
        this.shader = shader;
    }

    public void begin(Matrix4f camMatrix, Matrix4f projMatrix) {
        shader.bind();
        shader.loadMatrix("projMatrix", projMatrix);
        shader.loadMatrix("viewMatrix", camMatrix);
        shader.loadInt("useInputTransformationMatrix",0);
    }

    public void render(Model model, Matrix4f... modelMatrixArray) {
        model.renderInfo.actualVao.bind();
        int indiciesLength = model.renderInfo.actualVao.getIndicesLength();
        for (Matrix4f modelMatrix : modelMatrixArray) {
            shader.loadMatrix("transformationMatrixUniform", modelMatrix);
            glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, model.renderInfo.indexOffset * 4, model.renderInfo.vertexOffset);
            Main.inViewObjects++;
            Main.inViewVerticies += indiciesLength;
            Main.drawCalls++;
            Main.facesDrawn += indiciesLength / 3;
        }
        model.renderInfo.actualVao.unbind();
    }

    public void render(Map<Model, List<Entity>> toRender) {
        for (Model model : toRender.keySet()) {
            Vao mesh = model.renderInfo.actualVao;
            mesh.bind();
            int indiciesLength = mesh.getIndicesLength();
            for (Entity e : toRender.get(model)) {
                shader.loadMatrix("transformationMatrixUniform", e.getTransformationMatrix());
                glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, model.renderInfo.indexOffset * 4, model.renderInfo.vertexOffset);
                Main.inViewObjects++;
                Main.inViewVerticies += indiciesLength;
                Main.drawCalls++;
                Main.facesDrawn += indiciesLength / 3;
            }
            mesh.unbind();
        }
    }

}
