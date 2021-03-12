package graphics.camera;

import engine.input.InputMap;
import engine.input.MouseInputMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class FreeFlightCamera extends Camera {

    private final MouseInputMap movement;
    private final InputMap otherMovement;
    private float ySpeed;
    private float sideSpeed;
    private float xzSpeed;


    private final float normalSpeed = 500;
    private final float fastSpeed = 5000;

    private Vector3f lookDirection = new Vector3f(0, 0, 1);

    private final float turnSpeed = 1.6f / 50f;
    private final float upSpeed = 0.1f / 5f;

    public FreeFlightCamera(MouseInputMap mim, InputMap otherMovement) {
        this.movement = mim;
        this.otherMovement = otherMovement;
        position.set(0, 10, 0);
    }

    public void update(float dt) {
        float flySpeed = normalSpeed;
        if (otherMovement.getValueForAction("fastFlight") > 0) {
            flySpeed = fastSpeed;
        }
        if (otherMovement.getValueForAction("goLeft") > 0) {
            this.sideSpeed = flySpeed;
        } else if (otherMovement.getValueForAction("goRight") > 0) {
            this.sideSpeed = -flySpeed;
        } else {
            this.sideSpeed = 0;
        }
        if (otherMovement.getValueForAction("forward") > 0) {
            this.xzSpeed = flySpeed;
        } else if (otherMovement.getValueForAction("backward") > 0) {
            this.xzSpeed = -flySpeed;
        } else {
            this.xzSpeed = 0;
        }
        if (otherMovement.getValueForAction("up") > 0) {
            this.ySpeed = flySpeed;
        } else if (otherMovement.getValueForAction("down") > 0) {
            this.ySpeed = -flySpeed;
        } else {
            this.ySpeed = 0;
        }
        Vector3f up = new Vector3f(position).normalize();
        Vector3f right = up.cross(lookDirection, new Vector3f()).normalize();
        Vector3f forward = right.cross(up, new Vector3f()).normalize();
        lookDirection.add(new Vector3f(up).mul(movement.getValueForAction(MouseInputMap.MOUSE_DY) * upSpeed*dt));
        lookDirection.add(new Vector3f(right).mul(movement.getValueForAction(MouseInputMap.MOUSE_DX) * turnSpeed*dt));
        lookDirection.normalize();
        float length = position.length();
        position.add(forward.mul(xzSpeed*dt));
        //account for curvature in view direction
        Vector3f afterForwardTranslation = new Vector3f(position).normalize();
        if (up.distance(afterForwardTranslation) > 0) {
            float rotationAngle = afterForwardTranslation.dot(up);
            Vector3f rotationAxis = up.cross(afterForwardTranslation, new Vector3f());
            lookDirection.rotateAxis(rotationAngle, rotationAxis.x, rotationAxis.y, rotationAxis.z);
        }
        position.add(new Vector3f(right).mul(sideSpeed*dt));
        position.add(up.mul(ySpeed*dt));
    }

    @Override
    public Matrix4f getViewMatrix() {
        lookAt(new Vector3f(position).add(lookDirection));
        return super.getViewMatrix();
    }
}
