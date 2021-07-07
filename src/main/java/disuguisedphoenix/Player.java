package disuguisedphoenix;

import disuguisedphoenix.terrain.Island;
import engine.collision.CollisionShape;
import engine.input.InputMap;
import engine.input.MouseInputMap;
import graphics.camera.ThirdPersonCamera;
import graphics.modelinfo.Model;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class Player extends Entity {
    private static final float MAX_FLYSPEED = 1;
    private static final float TURN_SPEED_ACCEL = 7f;
    private static final float TILT_SPEED_ACCEL = 4f;

    private static final float MAX_TURN_SPEED = 50f;
    private static final float GRAVITY = 10;
    private final Vector3f currentTurnSpeed;
    private final Vector3i currentTurnDirection;
    private final Vector3f lookAtPosition = new Vector3f();
    public InputMap movement;
    public ThirdPersonCamera cam;
    Quaternionf rotation;
    Vector3f up = new Vector3f(), forward = new Vector3f(), right = new Vector3f();
    private float currentFlySpeed = 300;
    private float accelerate = 0;

    public Player(Model model, Vector3f position, MouseInputMap mim) {
        super(model, position, 0, 0, 0, 1f);
        cam = new ThirdPersonCamera(lookAtPosition, mim);
        rotation = new Quaternionf();
        this.velocity.z = MAX_FLYSPEED;
        currentTurnSpeed = new Vector3f();
        currentTurnDirection = new Vector3i();
    }

    public void move(List<Island> terrain, float dt, List<Entity> possibleCollisions, List<CollisionShape> shapes) {
        checkInputs();
        up = new Vector3f(position).normalize();
        this.acceleration.set(up).mul(-1f * GRAVITY);
        right = up.cross(velocity, new Vector3f()).normalize();
        forward = right.cross(up, new Vector3f()).normalize();
        rotation.identity();
        rotation.rotateTo(new Vector3f(0, 1, 0), position);
        Vector3f localForward = new Vector3f(0, 0, 1);
        rotation.transform(localForward);
        Quaternionf localYRotation = new Quaternionf().rotateTo(localForward, forward);
        Quaternionf localRotation = new Quaternionf().rotateTo(forward, velocity);
        localYRotation.mul(rotation, rotation);
        localRotation.mul(rotation, rotation);
        velocity.add(right.mul(currentTurnDirection.y * MAX_TURN_SPEED));
        velocity.add(up.mul(currentTurnDirection.x * MAX_TURN_SPEED));
        this.update(dt, possibleCollisions, shapes);
        cam.getPosition().set(new Vector3f(position).sub(new Vector3f(velocity).normalize(500)));
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
