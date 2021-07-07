package graphics.camera;

import engine.input.InputMap;
import engine.input.MouseInputMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FreeFlightCamera extends Camera {

    private static final float NORMAL_SPEED = 5;
    private static final float FAST_SPEED = 50;
    private static final float TURN_SPEED = 1.6f / 50f;
    private static final float UP_SPEED = 0.1f / 5f;
    private final MouseInputMap movement;
    private final InputMap otherMovement;
    private Vector3f lookDirection = new Vector3f(0, 0, 1);

    public FreeFlightCamera(MouseInputMap mim, InputMap otherMovement) {
        this.movement = mim;
        this.otherMovement = otherMovement;
        position.set(0, 10, 0);
    }

    @Override
    public void update(float dt) {
        float flySpeed = NORMAL_SPEED;
        if (otherMovement.getValueForAction("fastFlight") > 0) {
            flySpeed = FAST_SPEED;
        }
        float sideSpeed = 0;
        if (otherMovement.getValueForAction("goLeft") > 0) {
            sideSpeed = flySpeed;
        } else if (otherMovement.getValueForAction("goRight") > 0) {
            sideSpeed = -flySpeed;
        }
        float xzSpeed = 0;
        if (otherMovement.getValueForAction("forward") > 0) {
            xzSpeed = flySpeed;
        } else if (otherMovement.getValueForAction("backward") > 0) {
            xzSpeed = -flySpeed;
        }
        float ySpeed = 0;
        if (otherMovement.getValueForAction("up") > 0) {
            ySpeed = flySpeed;
        } else if (otherMovement.getValueForAction("down") > 0) {
            ySpeed = -flySpeed;
        }
        Vector3f up = new Vector3f(position).normalize();
        Vector3f right = up.cross(lookDirection, new Vector3f()).normalize();
        Vector3f forward = right.cross(up, new Vector3f()).normalize();
        lookDirection.add(new Vector3f(up).mul(movement.getValueForAction(MouseInputMap.MOUSE_DY) * UP_SPEED * dt));
        lookDirection.add(new Vector3f(right).mul(movement.getValueForAction(MouseInputMap.MOUSE_DX) * TURN_SPEED * dt));
        lookDirection.normalize();
        position.add(forward.mul(xzSpeed * dt));
        //account for curvature in view direction
        Vector3f afterForwardTranslation = new Vector3f(position).normalize();
        if (up.distance(afterForwardTranslation) > 0) {
            float rotationAngle = afterForwardTranslation.dot(up);
            Vector3f rotationAxis = up.cross(afterForwardTranslation, new Vector3f());
            lookDirection.rotateAxis(rotationAngle, rotationAxis.x, rotationAxis.y, rotationAxis.z);
        }
        position.add(new Vector3f(right).mul(sideSpeed * dt));
        position.add(up.mul(ySpeed * dt));
    }

    @Override
    public Matrix4f getViewMatrix() {
        lookAt(new Vector3f(position).add(lookDirection));
        return super.getViewMatrix();
    }
}
