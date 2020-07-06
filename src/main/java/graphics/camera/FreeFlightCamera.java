package graphics.camera;

import engine.input.InputMap;
import engine.input.KeyboardInputMap;
import engine.input.MouseInputMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class FreeFlightCamera extends Camera {

    Vector3f direction = new Vector3f(0, 0, -1);
    private MouseInputMap movement;
    private InputMap otherMovement;
    private float ySpeed;
    private float sideSpeed;
    private float xzSpeed;


    private float normalSpeed = 400;
    private float fastSpeed = 1000;

    private float angle = 0f;

    private float turnSpeed = 1.6f / 500f;
    private float upSpeed = 0.1f / 50f;

    public FreeFlightCamera(MouseInputMap mim, InputMap otherMovement) {
        this.movement = mim;
        this.otherMovement=otherMovement;
    }

    public void update(float dt) {
        float flySpeed=normalSpeed;
        if(otherMovement.getValueForAction("fastFlight")>0){
            flySpeed=fastSpeed;
        }
        if (otherMovement.getValueForAction("goLeft") > 0) {
            this.sideSpeed = -flySpeed;
        } else if (otherMovement.getValueForAction("goRight") > 0) {
            this.sideSpeed = flySpeed;
        } else {
            this.sideSpeed = 0;
        } if (otherMovement.getValueForAction("forward") > 0) {
            this.xzSpeed = flySpeed;
        } else if (otherMovement.getValueForAction("backward") > 0) {
            this.xzSpeed = -flySpeed;
        } else {
            this.xzSpeed = 0;
        } if (otherMovement.getValueForAction("up") > 0) {
            this.ySpeed = flySpeed;
        } else if (otherMovement.getValueForAction("down") > 0) {
            this.ySpeed = -flySpeed;
        } else {
            this.ySpeed = 0;
        }
        direction.y -= movement.getValueForAction(MouseInputMap.MOUSE_DY) * upSpeed;
        angle += movement.getValueForAction(MouseInputMap.MOUSE_DX) * turnSpeed;
        Vector2f rotation = createUnitVecFromAngle(angle);
        direction.x = rotation.x;
        direction.z = rotation.y;
        Vector2f sideways = createUnitVecFromAngle(angle+(float)Math.PI/2f);
        sideways.mul(sideSpeed*dt);
        position.x+=sideways.x;
        position.z+=sideways.y;
        Vector2f xzForward = createUnitVecFromAngle(angle);
        xzForward.mul(xzSpeed*dt);
        position.x+=xzForward.x;
        position.z+=xzForward.y;
        position.y += ySpeed *dt;
    }

    public static Vector2f createUnitVecFromAngle(float angle) {
        return new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
    }

    @Override
    public Matrix4f getViewMatrix() {
        lookAt(new Vector3f(position).add(direction));
        return super.getViewMatrix();
    }
}
