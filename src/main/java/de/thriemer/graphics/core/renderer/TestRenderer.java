package de.thriemer.graphics.core.renderer;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.Main;
import de.thriemer.graphics.core.objects.Vao;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.modelinfo.Model;
import de.thriemer.graphics.modelinfo.RenderInfo;
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
        RenderInfo renderInfo = model.getRenderInfo();
        Vao vao = model.getRenderInfo().getActualVao();
        vao.bind();
        int indicesLength = vao.getIndicesLength();
        for (Matrix4f modelMatrix : modelMatrixArray) {
            shader.loadMatrix("transformationMatrixUniform", modelMatrix);
            glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indicesLength, GL11.GL_UNSIGNED_INT, renderInfo.getIndexOffset() * 4L, renderInfo.getVertexOffset());
            Main.inViewObjects++;
            Main.inViewVerticies += indicesLength;
            Main.drawCalls++;
            Main.facesDrawn += indicesLength / 3;
        }
       vao.unbind();
    }

    public void render(Map<Model, List<Entity>> toRender) {
        for (Model model : toRender.keySet()) {
            RenderInfo renderInfo = model.getRenderInfo();
            Vao mesh = renderInfo.getActualVao();
            mesh.bind();
            int indicesLength = mesh.getIndicesLength();
            for (Entity e : toRender.get(model)) {
                shader.loadMatrix("transformationMatrixUniform", e.getTransformationMatrix());
                glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indicesLength, GL11.GL_UNSIGNED_INT, renderInfo.getIndexOffset() * 4L, renderInfo.getVertexOffset());
                Main.inViewObjects++;
                Main.inViewVerticies += indicesLength;
                Main.drawCalls++;
                Main.facesDrawn += indicesLength / 3;
            }
            mesh.unbind();
        }
    }

}
