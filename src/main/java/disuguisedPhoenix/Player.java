package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Terrain;
import engine.input.InputMap;
import engine.input.MouseInputMap;
import engine.util.Maths;
import graphics.camera.ThirdPersonCamera;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;

public class Player extends Entity {
    private static final float MAX_FLYSPEED = 400;
    private static final float TURN_SPEED_ACCEL = 7f;
    private static final float TILT_SPEED_ACCEL = 4f;

    private static final float MAX_TURN_SPEED = 3f;
    private static final float GRAVITY = -600;

    Quaternionf rotation;

    private Vector3f currentTurnSpeed;
    private Vector3i currentTurnDirection;

    private float propRotation;
    Quaternionf propellorWorldRotation=new Quaternionf();

    private float currentFlySpeed = 300;

    public InputMap movement;
    public ThirdPersonCamera cam;
    private Vector3f lookAtPosition = new Vector3f();

    private float accelerate=0;

    public Player(Model model, Vector3f position, MouseInputMap mim) {
        super(model, position, 0, 0, 0, 0.1f);
        cam = new ThirdPersonCamera(lookAtPosition, mim);
        rotation = new Quaternionf();
        this.velocity.z = 100;
        currentTurnSpeed = new Vector3f();
        currentTurnDirection = new Vector3i();
    }

    public void move(Terrain terrain, float dt, List<Entity> possibleCollisions) {
        checkInputs();
        int correctionDirectionY = Math.abs(currentTurnSpeed.y) < 0.05f ? 0 : currentTurnSpeed.y > 0 ? -1 : 1;
        int wantedTurnDirectionY = currentTurnDirection.y != 0 ? currentTurnDirection.y : correctionDirectionY;
        currentTurnSpeed.y += wantedTurnDirectionY * TURN_SPEED_ACCEL * dt;
        currentTurnSpeed.y = Maths.clamp(currentTurnSpeed.y, -MAX_TURN_SPEED, MAX_TURN_SPEED);
        int correctionDirectionX = Math.abs(currentTurnSpeed.x) < 0.001f ? 0 : currentTurnSpeed.x > 0 ? -1 : 1;
        int wantedTurnDirectionX = currentTurnDirection.x != 0 ? currentTurnDirection.x : correctionDirectionX;
        currentTurnSpeed.x += wantedTurnDirectionX * TILT_SPEED_ACCEL * dt;
        currentTurnSpeed.x = Maths.clamp(currentTurnSpeed.x, -MAX_TURN_SPEED, MAX_TURN_SPEED);
        rotY += currentTurnSpeed.y * dt;
        rotX += currentTurnSpeed.x * dt;
        propRotation+=36f*dt+accelerate*20f*dt;
        rotX = Maths.clamp(rotX, -(float) Math.PI / 3f, (float) Math.PI / 2f-0.0001f);
        rotation.identity();
        rotation.rotateYXZ(rotY, rotX, rotZ);
        propellorWorldRotation.identity();
        propellorWorldRotation.rotateYXZ(rotY, rotX, propRotation);
        Vector3f forward = rotation.transform(new Vector3f(0, 0, 1));
        currentFlySpeed += forward.y * dt * GRAVITY;
        currentFlySpeed += accelerate*dt*250;
        float speedDecay=0.99f;
        currentFlySpeed = currentFlySpeed*speedDecay+MAX_FLYSPEED*(1f-speedDecay);
        currentFlySpeed = Math.max(currentFlySpeed, 400);
        velocity.set(new Vector3f(forward).mul(currentFlySpeed));
        super.update(dt,possibleCollisions);
        rotZ = Maths.map(-currentTurnSpeed.y, -MAX_TURN_SPEED, MAX_TURN_SPEED, -(float) Math.PI / 4f, (float) Math.PI / 4f);
        lookAtPosition.set(new Vector3f(position)/*.normalize().mul(30).add(position)*/);
        cam.position.set(new Vector3f(position).add(forward.mul(-5f)));
    }

    public Matrix4f getPropellorMatrix(){
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotate(propellorWorldRotation);
        modelMatrix.scale(scale);
        return modelMatrix;
    }


    private void checkInputs() {
        if (movement.getValueForAction("forward") > 0) {
            this.currentTurnDirection.x = 1;
        } else if (movement.getValueForAction("backward") > 0) {
            this.currentTurnDirection.x = -1;
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
        accelerate=movement.getValueForAction("accel");
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
