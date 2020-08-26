package engine.util;

import org.joml.FrustumIntersection;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Maths {

    private static Vector3f tempVec=new Vector3f();

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


}
