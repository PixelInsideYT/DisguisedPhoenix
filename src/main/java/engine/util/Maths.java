package engine.util;

import disuguisedphoenix.Entity;
import org.joml.*;

import java.lang.Math;

public class Maths {

    private static final Vector3f tempVec=new Vector3f();

    public static Vector2f raySphere(Vector3f center, float radius, Vector3f rayOrigin, Vector3f rayDir) {
        Vector3f offset = new Vector3f(rayOrigin).sub(center);
        float a = rayDir.dot(rayDir);
        float b = 2 *offset.dot(rayDir);
        float c = offset.dot( offset) - radius * radius;
        float discriminant = b * b - 4 * a * c;
        if (discriminant > 0) {
            float s = (float) Math.sqrt(discriminant);
            float dstToSphereNear = Math.max(0, (-b - s) / (2 * a));
            float dstToSphereFar = (s - b) / (2 * a);
            if (dstToSphereFar >= 0) {
                return new Vector2f(dstToSphereNear, dstToSphereFar - dstToSphereNear);
            }
        }
        return new Vector2f(Float.MAX_VALUE, 0f);
    }

    public static float clamp(float currentTurnSpeed, float min, float max) {
        return Math.max(Math.min(currentTurnSpeed, max), min);
    }

    public static float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static Vector2f createUnitVecFromAngle(float angle) {
        return new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
    }

    public static boolean isInsideFrustum(FrustumIntersection cullingHelper,Vector3f pos, Vector3f ralativeCenter, float scale, float radius) {
        Vector3f p = new Vector3f(pos).add(scale*ralativeCenter.x,scale*ralativeCenter.y,scale*ralativeCenter.z);
        return cullingHelper.testSphere(p, radius * scale);
    }

    public static boolean isInsideFrustum(FrustumIntersection cullingHelper, Entity e){
        return isInsideFrustum(cullingHelper,e.getPosition(),e.getModel().relativeCenter,e.getScale(),e.getModel().radius);
    }

    public static Matrix4f lookAt(Vector3f target, Vector3f position) {
        Vector3f forward = new Vector3f(position).sub(target).normalize();
        Vector3f right = new Vector3f(position).normalize().cross(forward).normalize();
        Vector3f up = new Vector3f(forward).cross(right).normalize();
        Matrix4f lookingMat = new Matrix4f();
        lookingMat.setRow(0, new Vector4f(right, -position.dot(right)));
        lookingMat.setRow(1, new Vector4f(up, -position.dot(up)));
        lookingMat.setRow(2, new Vector4f(forward, -position.dot(forward)));
        return lookingMat;
    }

}
