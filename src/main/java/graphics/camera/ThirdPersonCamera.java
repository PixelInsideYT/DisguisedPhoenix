package graphics.camera;

import engine.input.MouseInputMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ThirdPersonCamera extends Camera {

    private Vector3f attachPosition;

    public ThirdPersonCamera(Vector3f attachTo, MouseInputMap mim) {
        super();
        attachPosition = attachTo;
    }

    @Override
    public void update(float dt) {
        lookAt(attachPosition);
        this.viewMatrix.rotateXYZ(rotation);
    }
}
