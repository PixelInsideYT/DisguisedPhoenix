package de.thriemer.engine.util;

import de.thriemer.disguisedphoenix.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Maths {

    public static Vector2f raySphere(Vector3f center, float radius, Vector3f rayOrigin, Vector3f rayDir) {
        Vector3f offset = new Vector3f(rayOrigin).sub(center);
        float a = rayDir.dot(rayDir);
        float b = 2 * offset.dot(rayDir);
        float c = offset.dot(offset) - radius * radius;
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

    public static float map(float x, float inMin, float inMax, float outMin, float outMax) {
        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }

    public static Vector2f createUnitVecFromAngle(float angle) {
        return new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
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

    public static float sdfBox(Vector3f boxPosition, float halfWidth, float halfHeight, float halfDepth, Vector3f point) {
        Vector3f offset = new Vector3f(point).sub(boxPosition).absolute()
                .sub(new Vector3f(halfWidth, halfHeight, halfDepth));
        float unsignedDistance = new Vector3f(offset).max(new Vector3f(0)).length();
        offset.min(new Vector3f(0));
        float distanceInsideBox = offset.get(offset.maxComponent());
        return unsignedDistance + distanceInsideBox;
    }

    private static final float NEEDED_SIZE_PER_LENGTH_UNIT = 0.005f;

    public static boolean couldBeVisible(Entity e, Vector3f cameraPos) {
        float size = e.getRadius();
        float distance = e.getPosition().distance(cameraPos);
        return size > distance * NEEDED_SIZE_PER_LENGTH_UNIT;
    }


    public static boolean aabbFullyContainsSphere(Vector3f min, Vector3f max, Vector3f center, float radius) {
        return min.x < center.x - radius && center.x + radius < max.x &&
                min.y < center.y - radius && center.y + radius < max.y &&
                min.z < center.z - radius && center.z + radius < max.z;
    }

    public static boolean pointInAabb(Vector3f min, Vector3f max, Vector3f center) {
        return min.x < center.x && center.x < max.x &&
                min.y < center.y && center.y < max.y &&
                min.z < center.z && center.z < max.z;
    }
}
