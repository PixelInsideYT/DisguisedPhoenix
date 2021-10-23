package de.thriemer.graphics.occlusion;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ShadowCascade {

    private final Matrix4f lightProjMatrix;
    private final Matrix4f lightViewMatrix;

    private final Vector3f[] frustumCorners;
    private final Vector3f centroid;


    public ShadowCascade() {
        frustumCorners = new Vector3f[8];
        centroid = new Vector3f();
        for (int i = 0; i < frustumCorners.length; i++) frustumCorners[i] = new Vector3f();
        lightProjMatrix = new Matrix4f();
        lightViewMatrix = new Matrix4f();
    }

    protected void update(Matrix4f viewMatrix, float near, float far, float fov, float aspect, Vector3f lightPos) {
        // Calculate frustum corners in world space
        float maxZ = Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE;
        Matrix4f projViewMatrix = new Matrix4f().perspective(fov, aspect, near, far).mul(viewMatrix);
        for (int i = 0; i < frustumCorners.length; i++) {
            Vector3f corner = frustumCorners[i];
            projViewMatrix.frustumCorner(i, corner);
            centroid.add(corner);
            centroid.div(2.0f);
            minZ = Math.min(minZ, corner.z);
            maxZ = Math.max(maxZ, corner.z);
        }

        // Go back from the centroid up to max.z - min.z in the direction of light
        Vector3f lightDirection = new Vector3f(lightPos).normalize();
        Vector3f lightPosInc = new Vector3f().set(lightDirection);
        float distance = maxZ - minZ;
        lightPosInc.mul(distance);
        Vector3f lightPosition = new Vector3f();
        lightPosition.set(centroid);
        lightPosition.add(lightPosInc);
        updateLightViewMatrix(lightDirection, lightPosition);
        updateLightProjectionMatrix();
    }

    private void updateLightViewMatrix(Vector3f lightDirection, Vector3f lightPosition) {
        float lightAngleX = (float) Math.toDegrees(Math.acos(lightDirection.z));
        float lightAngleY = (float) Math.toDegrees(Math.asin(lightDirection.x));
        lightViewMatrix.rotationX((float) Math.toRadians(lightAngleX))
                .rotateY((float) Math.toRadians(lightAngleY))
                .translate(-lightPosition.x, -lightPosition.y, -lightPosition.z);
    }

    private void updateLightProjectionMatrix() {
        // Now calculate frustum dimensions in light space
        Vector4f tmpVec = new Vector4f();
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MIN_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MIN_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MIN_VALUE;
        for (Vector3f corner : frustumCorners) {
            tmpVec.set(corner, 1);
            tmpVec.mul(lightViewMatrix);
            minX = Math.min(tmpVec.x, minX);
            maxX = Math.max(tmpVec.x, maxX);
            minY = Math.min(tmpVec.y, minY);
            maxY = Math.max(tmpVec.y, maxY);
            minZ = Math.min(tmpVec.z, minZ);
            maxZ = Math.max(tmpVec.z, maxZ);
        }
        float distz = maxZ - minZ;
        lightProjMatrix.setOrtho(minX, maxX, minY, maxY, 0, distz);
    }

    public Matrix4f getShadowProjViewMatrix() {
        Matrix4f offset = new Matrix4f();
        offset.translate(new Vector3f(0.5f, 0.5f, 0.5f));
        offset.scale(new Vector3f(0.5f, 0.5f, 0.5f));
        return offset.mul(lightProjMatrix).mul(lightViewMatrix);
    }

    public Matrix4f getViewProjMatrix() {
        return new Matrix4f(lightProjMatrix).mul(lightViewMatrix);
    }

}
