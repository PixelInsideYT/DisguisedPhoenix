package engine.util;

import disuguisedPhoenix.Entity;
import org.joml.*;

import java.lang.Math;

public class Maths {

    private static final Vector3f tempVec=new Vector3f();

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
        tempVec.set(pos).add(scale*ralativeCenter.x,scale*ralativeCenter.y,scale*ralativeCenter.z);
        return cullingHelper.testSphere(tempVec, radius * scale);
    }

    public static boolean isInsideFrustum(FrustumIntersection cullingHelper, Entity e){
        return isInsideFrustum(cullingHelper,e.position,e.getModel().relativeCenter,e.scale,e.getModel().radius);
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
