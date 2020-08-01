package graphics.renderer;

import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.world.Material;
import graphics.world.Mesh;
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
        for (Mesh m : model.meshes) {
            Vao toRender = m.mesh;
            toRender.bind();
            Material mat = Material.allMaterials.get(m.materialName);
            bindMaterial(mat, shader);
            for (Matrix4f modelMatrix : modelMatrixArray) {
                shader.loadMatrix("transformationMatrix", modelMatrix);
                GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
            }
            toRender.unbind();
        }
    }

    private void bindMaterial(Material mat, Shader shader) {
        if (mat != currentMaterial) {
            currentMaterial = mat;
           // shader.load3DVector("diffuse", mat.diffuse);
            shader.load3DVector("ambient", mat.ambient);
            shader.load3DVector("specular", mat.specular);
            shader.loadFloat("shininess", mat.shininess);
            shader.loadFloat("opacity", mat.opacity);
        }
    }

    public void end() {
        shader.unbind();
    }

}
