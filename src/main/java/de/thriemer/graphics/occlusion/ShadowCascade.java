package de.thriemer.graphics.occlusion;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.CallbackI;

import static org.joml.Matrix4fc.*;

public class ShadowCascade {

    public static final float LIGHT_OFFSET=10;

    private final Matrix4f lightProjMatrix;
    private final Matrix4f lightViewMatrix;

    private final Vector3f[] frustumCorners;

    public ShadowCascade() {
        frustumCorners = new Vector3f[8];
        for (int i = 0; i < frustumCorners.length; i++) frustumCorners[i] = new Vector3f();
        lightProjMatrix = new Matrix4f();
        lightViewMatrix = new Matrix4f();
    }
    Vector3f lightPosition = new Vector3f();
    private static final int BOT_LEFT = CORNER_NXPYNZ;// left, top, near
    private static final int TOP_LEFT = CORNER_NXPYPZ; // left, top, far
    private static final int TOP_RIGHT = CORNER_PXPYPZ; // right, top, far

    public float size;
    protected void update(Matrix4f viewMatrix, float near, float far, float fov, float aspect, Vector3f lightPos) {
        // Calculate frustum corners in world space
        Vector3f center = new Vector3f();
        Matrix4f projViewMatrix = new Matrix4f().perspective((float)Math.toRadians(fov), aspect, near, far).mul(viewMatrix);
        for (int i = 0; i < frustumCorners.length; i++) {
            projViewMatrix.frustumCorner(i, frustumCorners[i]);
            center.add(frustumCorners[i]);
        }
        size=new Vector3f(frustumCorners[TOP_RIGHT]).sub(frustumCorners[TOP_LEFT]).cross(new Vector3f(frustumCorners[BOT_LEFT]).sub(frustumCorners[TOP_LEFT])).length();
        center.div(8f);
        // Go back from the centroid up to max.z - min.z in the direction of light
        Vector3f lightDirection = new Vector3f(lightPos).normalize().mul(LIGHT_OFFSET);
        lightViewMatrix.identity();
        lightViewMatrix.lookAt(new Vector3f(center).add(lightDirection),center,new Vector3f(0,1,0));
        updateLightProjectionMatrix();
    }

    private void updateLightProjectionMatrix() {
        // Now calculate frustum dimensions in light space
        Vector3f tmpVec = new Vector3f();
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);

        for (Vector3f corner : frustumCorners) {
            lightViewMatrix.transformPosition(corner,tmpVec);
            min.min(tmpVec);
            max.max(tmpVec);
        }
        lightProjMatrix.setOrtho(min.x,max.x,min.y,max.y,0,max.z-min.z+LIGHT_OFFSET);
    }

    public Matrix4f getShadowProjViewMatrix() {
        Matrix4f offset = new Matrix4f();
        offset.translate(0.5f, 0.5f, 0.5f);
        offset.scale(0.5f, 0.5f, 0.5f);
        return offset.mul(lightProjMatrix).mul(lightViewMatrix);
    }

    public Matrix4f getProjViewMatrix() {
        return new Matrix4f(lightProjMatrix).mul(lightViewMatrix);
    }

    public Matrix4f getProjMatrix(){
        return lightProjMatrix;
    }

    public Matrix4f getViewMatrix(){
        return lightViewMatrix;
    }

    public Vector3f getLightPosition() {
        return lightPosition;
    }
}
