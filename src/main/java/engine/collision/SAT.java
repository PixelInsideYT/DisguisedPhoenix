package engine.collision;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SAT {

    public static Vector3f getMTV(Collider moving, Vector3f velocity, Collider staticShape, float dt) {
        if (getMTVNarrow(moving.boundingBox, velocity, staticShape.boundingBox, dt) == null) {
            return null;
        }
        if (velocity.length() == 0) {
            return getMTV(moving, staticShape);
        }
        Vector3f mtv = null;
        float length = 0;
        for (CollisionShape csMoving : moving.allTheShapes) {
            for (CollisionShape csStatic : staticShape.allTheShapes) {
                Vector3f potentialNewMtv = getMTVNarrow(csMoving, velocity, csStatic, dt);
                if (potentialNewMtv != null && potentialNewMtv.length() > length) {
                    if (mtv == null) {
                        mtv = new Vector3f();
                    }
                    mtv.set(potentialNewMtv);
                    length = mtv.length();
                }
            }
        }
        return mtv;
    }

    public static Vector3f getMTV(Collider moving, Collider staticShape) {
        if (getMTVNarrow(moving.boundingBox, staticShape.boundingBox) == null) {
            return null;
        }
        Vector3f mtv = null;
        float length = 0;
        for (CollisionShape csMoving : moving.allTheShapes) {
            for (CollisionShape csStatic : staticShape.allTheShapes) {
                Vector3f potentialNewMtv = getMTVNarrow(csMoving, csStatic);
                if (potentialNewMtv != null && potentialNewMtv.length() > length) {
                    if (mtv == null) {
                        mtv = new Vector3f();
                    }
                    mtv.set(potentialNewMtv);
                    length = mtv.length();
                }
            }
        }
        return mtv;
    }

    public static Vector3f getMTVNarrow(CollisionShape movingShape, Vector3f vel, CollisionShape staticShape, float dt) {
        Vector3f mtv = new Vector3f();
        List<Vector3f> axisToTest = new ArrayList<>();
        axisToTest.addAll(Arrays.asList(movingShape.getAxis()));
        axisToTest.addAll(Arrays.asList(staticShape.getAxis()));
        float tFirstEntrance = Float.MAX_VALUE;
        float tExit = Float.MAX_VALUE;
        for (Vector3f axis : axisToTest) {
            float speed = axis.dot(vel);
            Vector2f movinProj = movingShape.projectOnAxis(axis);
            Vector2f staticProj = staticShape.projectOnAxis(axis);
            boolean isOverlapping = isOverlapping(movinProj, staticProj);
            if (speed == 0f) {
                if (!isOverlapping) {
                    return null;
                }
            } else {
                float smallestOverlap = returnSmallestOverlap(movinProj, staticProj);
                boolean isOnLeft = isOnLeft(movinProj, staticProj);
                float biggestOverlap = getBiggestOverlap(movinProj, staticProj);
                float leavingDistance = getLeavingDistance(speed, smallestOverlap, biggestOverlap, isOnLeft);
                float tEntranceCurrent = entranceDistance(smallestOverlap, biggestOverlap, speed, isOnLeft) / speed;
                tExit = Math.min(tExit, leavingDistance / speed);
                if ((tEntranceCurrent < 0 && !isOverlapping) || (tEntranceCurrent > dt)) {
                    return null;
                }
                if (Math.abs(tFirstEntrance) > Math.abs(tEntranceCurrent)) {
                    tFirstEntrance = tEntranceCurrent;
                    mtv.set(axis);
                    mtv.mul(-speed * (dt - tFirstEntrance));
                }
                if (tFirstEntrance > tExit) {
                    return null;
                }
            }
        }
        return mtv;
    }

    private static float entranceDistance(float smallestOverlap, float biggestOverlap, float speed, boolean isOnLeft) {
        if (isOnLeft) {
            return speed > 0 ? smallestOverlap : biggestOverlap;
        } else {
            return speed > 0 ? -biggestOverlap : smallestOverlap;
        }
    }

    private static boolean isOnLeft(Vector2f movinProj, Vector2f staticProj) {
        if (isOverlapping(movinProj, staticProj)) {
            float toLeft = staticProj.x - movinProj.y;
            float toRight = staticProj.y - movinProj.x;
            return Math.abs(toLeft) < Math.abs(toRight);
        }
        return movinProj.x < staticProj.x;
    }

    public static Vector3f getMTVNarrow(CollisionShape movable, CollisionShape staticShape) {
        Vector3f mtv = new Vector3f();
        float smallestOverlap = Float.MAX_VALUE;
        List<Vector3f> axisToTest = new ArrayList<>();
        axisToTest.addAll(Arrays.asList(movable.getAxis()));
        axisToTest.addAll(Arrays.asList(staticShape.getAxis()));
        for (Vector3f axis : axisToTest) {
            Vector2f movProj = movable.projectOnAxis(axis);
            Vector2f staticProj = staticShape.projectOnAxis(axis);
            if (isOverlapping(movProj, staticProj)) {
                float currentOverlap = returnSmallestOverlap(movProj, staticProj);
                if (Math.abs(smallestOverlap) >= Math.abs(currentOverlap)) {
                    mtv.set(axis).mul(currentOverlap);
                    smallestOverlap = currentOverlap;
                }
            } else {
                return null;
            }
        }
        return mtv;
    }

    private static boolean isOverlapping(Vector2f pp1, Vector2f pp2) {
        return !(pp2.x >= pp1.y || pp1.x >= pp2.y);
    }

    private static float getLeavingDistance(float projSpeed, float smallestOverlap, float biggestOverlap, boolean isOnLeft) {
        // movable is on the left from static object
        if (isOnLeft) {
            return projSpeed > 0 ? biggestOverlap : smallestOverlap;
            //movable is on the right from static object
        } else {
            //minus for correct time calculations
            return projSpeed > 0 ? smallestOverlap : -biggestOverlap;
        }
    }

    private static float getBiggestOverlap(Vector2f pp1, Vector2f pp2) {
        float x3 = Math.min(pp1.x, pp2.x);
        float y3 = Math.max(pp1.y, pp2.y);
        return y3 - x3;
    }

    private static float returnSmallestOverlap(Vector2f pp1, Vector2f pp2) {
        if (projectionIsInsideProjection(pp1, pp2)) {
            float toLeft = pp2.x - pp1.y;
            float toRight = pp2.y - pp1.x;
            return Math.abs(toLeft) < Math.abs(toRight) ? toLeft : toRight;
        } else {
            float x3 = Math.max(pp1.x, pp2.x);
            float y3 = Math.min(pp1.y, pp2.y);
            float smallestOverlap = x3 - y3;
            //invert overlap to seprate in right direction, therefore no need to correct the mtv
            if (pp1.x > pp2.x) {
                smallestOverlap *= -1f;
            }
            return smallestOverlap;
        }
    }

    private static boolean projectionIsInsideProjection(Vector2f pp1, Vector2f pp2) {
        return (pp1.x < pp2.x && pp2.y < pp1.y) || (pp2.x < pp1.x && pp1.y < pp2.y);
    }

}
