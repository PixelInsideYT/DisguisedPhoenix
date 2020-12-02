package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Island;
import engine.input.InputMap;
import engine.input.MouseInputMap;
import engine.util.Maths;
import graphics.camera.ThirdPersonCamera;
import graphics.world.Model;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Player extends Entity {
    private static final float MAX_FLYSPEED = 300;
    private static final float TURN_SPEED_ACCEL = 7f;
    private static final float TILT_SPEED_ACCEL = 4f;

    private static final float MAX_TURN_SPEED = 50f;
    private static final float GRAVITY = 10;
    public InputMap movement;
    public ThirdPersonCamera cam;
    Quaternionf rotation;
    private final Vector3f currentTurnSpeed;
    private final Vector3i currentTurnDirection;
    private float currentFlySpeed = 300;
    private final Vector3f lookAtPosition = new Vector3f();

    private float accelerate = 0;


    public Player(Model model, Vector3f position, MouseInputMap mim) {
        super(model, position, 0, 0, 0, 10f);
        cam = new ThirdPersonCamera(lookAtPosition, mim);
        rotation = new Quaternionf();
        this.velocity.z = MAX_FLYSPEED;
        currentTurnSpeed = new Vector3f();
        currentTurnDirection = new Vector3i();
    }

    public void move(List<Island> terrain, float dt, List<Entity> possibleCollisions) {
        checkInputs();
        Vector3f up = new Vector3f(position).normalize();
        this.acceleration.set(up).mul(-1f*GRAVITY);
        Vector3f right = up.cross(velocity,new Vector3f()).normalize();
        Vector3f forward = right.cross(up, new Vector3f()).normalize();
        rotation.identity();
        rotation.rotateTo(new Vector3f(0,1,0),position);
        velocity.add(right.mul(currentTurnDirection.y*MAX_TURN_SPEED));
        velocity.add(up.mul(currentTurnDirection.x*MAX_TURN_SPEED));
        this.update(dt,possibleCollisions);
        cam.position.set(new Vector3f(position).sub(new Vector3f(velocity).normalize(500)));
        lookAtPosition.set(position);
    }


    private void checkInputs() {
        if (movement.getValueForAction("forward") > 0) {
            this.currentTurnDirection.x = -1;
        } else if (movement.getValueForAction("backward") > 0) {
            this.currentTurnDirection.x = 1;
        } else {
            currentTurnDirection.x = 0;
        }
        if (movement.getValueForAction("turnLeft") > 0) {
            this.currentTurnDirection.y = 1;
        } else if (movement.getValueForAction("turnRight") > 0) {
            this.currentTurnDirection.y = -1;
        } else {
            this.currentTurnDirection.y = 0;
        }
        accelerate = movement.getValueForAction("accel");
    }

    @Override
    public final Matrix4f getTransformationMatrix() {
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotate(rotation);
        modelMatrix.scale(scale);
        return modelMatrix;
    }
}
