package engine.util;

import org.joml.Vector2f;

public class Maths {


    public static float clamp(float currentTurnSpeed, float min, float max) {
        return Math.max(Math.min(currentTurnSpeed,max),min);
    }

    public static float map(float x, float in_min, float in_max, float out_min, float out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
    public static Vector2f createUnitVecFromAngle(float angle) {
        return new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
    }


}
