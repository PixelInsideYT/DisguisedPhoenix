package graphics.renderer;

import graphics.world.Model;
import org.joml.Matrix4f;

public interface IRenderer {

    void begin(Matrix4f camMatrix, Matrix4f projMatrix) ;
    void render(Model model, Matrix4f... modelMatrixArray);
    void end();


}
