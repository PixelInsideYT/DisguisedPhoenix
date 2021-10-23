package de.thriemer.graphics.camera;

import de.thriemer.engine.input.MouseInputMap;
import org.joml.Vector3f;

public class ThirdPersonCamera extends Camera {

    private final Vector3f attachPosition;

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
