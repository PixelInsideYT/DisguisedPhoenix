package de.thriemer.disguisedphoenix.rendering;

import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Getter
public class CameraInformation {

    private float farPlane;
    private float nearPlane;
    private float fov;

    private Matrix4f viewMatrix=new Matrix4f();
    private Matrix4f projectionMatrix=new Matrix4f();
    private Matrix4f invertedProjectionMatrix=new Matrix4f();
    private Matrix4f projViewMatrix=new Matrix4f();

    public CameraInformation(float nearPlane, float farPlane, float fov, float aspectRatio) {
        update(nearPlane,farPlane,fov,aspectRatio);
    }

    public void update( float nearPlane,float farPlane, float fov,float aspectRatio) {
        this.farPlane = farPlane;
        this.nearPlane = nearPlane;
        this.fov = fov;
        projectionMatrix.identity().perspective((float)Math.toRadians(fov),aspectRatio,nearPlane,farPlane);
        invertedProjectionMatrix.set(projectionMatrix).invert();
        recalculateProjViewMatrix();
    }

    public void updateCameraMatrix(Matrix4f viewMatrix){
        this.viewMatrix.set(viewMatrix);
        recalculateProjViewMatrix();
    }

    public Vector3f getCameraPosition(){
       return viewMatrix.getTranslation(new Vector3f()).negate();
    }

    private void recalculateProjViewMatrix(){
        projViewMatrix.set(projectionMatrix).mul(viewMatrix);
    }

}
