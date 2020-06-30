package graphics.camera;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Camera {

    Matrix4f viewMatrix;
    public Vector3f position;
    public Vector3f rotation;

    public Camera() {
        viewMatrix = new Matrix4f();
        position = new Vector3f();
        rotation = new Vector3f();
    }

    public void update(float dt){}

    public void lookAt(Vector3f target){
        Vector3f forward = new Vector3f(position).sub(target).normalize();
        Vector3f right = new Vector3f(0,1,0f).cross(forward).normalize();
        Vector3f up = new Vector3f(forward).cross(right).normalize();
        viewMatrix.identity();
        viewMatrix.setRow(0,new Vector4f(right,-position.dot(right)));
        viewMatrix.setRow(1,new Vector4f(up,-position.dot(up)));
        viewMatrix.setRow(2,new Vector4f(forward,-position.dot(forward)));
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

}
