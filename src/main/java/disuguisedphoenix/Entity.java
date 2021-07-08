package disuguisedphoenix;

import engine.collision.Collider;
import engine.collision.CollisionShape;
import engine.collision.SAT;
import graphics.modelinfo.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

public class Entity {

    private final Model model;
    protected Vector3f position;
    protected Vector3f velocity;
    protected Vector3f acceleration;
    protected Matrix4f modelMatrix;
    float rotX;
    float rotY;
    float rotZ;
    float scale;
    boolean changedPosition = true;
    private Collider transformedCollider;

    public Entity(Model model, Vector3f position, float rotX, float rotY, float rotZ, float scale) {
        super();
        this.model = model;
        this.position = position;
        this.velocity = new Vector3f();
        this.acceleration = new Vector3f();
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.scale = scale;
        modelMatrix = new Matrix4f();
    }

    public Matrix4f getTransformationMatrix() {
        if (changedPosition) {
            modelMatrix.identity();
            modelMatrix.translate(position);
            modelMatrix.rotateXYZ(new Vector3f(rotX, rotY, rotZ));
            modelMatrix.scale(scale);
            changedPosition = false;
        }
        return modelMatrix;
    }

    public void update(float dt, List<Entity> possibleCollisions, List<CollisionShape> shapes) {
        velocity.add(new Vector3f(acceleration).mul(dt));
        if (velocity.length() > 0) {
            changedPosition = true;
        }
        Collider own = getCollider();
        if (own != null) {
            for (Entity e : possibleCollisions) {
                Collider eCollider = e.getCollider();
                if (eCollider != null) {
                    Vector3f mtv = SAT.getMTV(own, velocity, eCollider, dt);
                    if (mtv != null) {
                        position.add(mtv);
                    }
                }
            }
            for (CollisionShape cs : shapes) {
                for (CollisionShape ownShape : own.getAllTheShapes()) {
                    Vector3f mtv = SAT.getMTVNarrow(ownShape, velocity, cs, dt);
                    if (mtv != null) {
                        position.add(mtv);
                    }
                }
            }
        }
        position.add(new Vector3f(velocity).mul(dt));
    }

    public Model getModel() {
        return model;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public float getRotX() {
        return rotX;
    }

    public void setRotX(float rotX) {
        this.rotX = rotX;
    }

    public float getRotY() {
        return rotY;
    }

    public void setRotY(float rotY) {
        this.rotY = rotY;
    }

    public float getRotZ() {
        return rotZ;
    }

    public void setRotZ(float rotZ) {
        this.rotZ = rotZ;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Collider getCollider() {
        if (model.collider != null) {
            if (transformedCollider == null || changedPosition) {
                transformedCollider = model.collider.cloneAndTransform(getTransformationMatrix());
            }
            return transformedCollider;
        }
        return null;
    }

    public void lookIntoDirection(Vector3f direction) {
        Vector3f forward = new Vector3f(direction).normalize();
        Vector3f right = new Vector3f(0, 1, 0).cross(forward).normalize();
        Vector3f up = new Vector3f(forward).cross(right).normalize();
        modelMatrix.setRow(0, new Vector4f(right, -position.dot(right)));
        modelMatrix.setRow(1, new Vector4f(up, -position.dot(up)));
        modelMatrix.setRow(2, new Vector4f(forward, -position.dot(forward)));
    }

    public Vector3f getCenter(){
        getTransformationMatrix();
        return modelMatrix.transformPosition(new Vector3f(model.relativeCenter));
    }

    public float getRadius(){
        return scale* model.radius;
    }

}
