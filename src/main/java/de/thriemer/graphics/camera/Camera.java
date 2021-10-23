package de.thriemer.graphics.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class Camera {

    protected Vector3f position;
    protected Vector3f rotation;
    Matrix4f viewMatrix;

    protected Camera() {
        viewMatrix = new Matrix4f();
        position = new Vector3f();
        rotation = new Vector3f();
    }

    public abstract void update(float dt);

    public void lookAt(Vector3f target) {
        Vector3f forward = new Vector3f(position).sub(target).normalize();
        Vector3f right = new Vector3f(position).normalize().cross(forward).normalize();
        Vector3f up = new Vector3f(forward).cross(right).normalize();
        viewMatrix.identity();
        viewMatrix.setRow(0, new Vector4f(right, -position.dot(right)));
        viewMatrix.setRow(1, new Vector4f(up, -position.dot(up)));
        viewMatrix.setRow(2, new Vector4f(forward, -position.dot(forward)));
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }
}
