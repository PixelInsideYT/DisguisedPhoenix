package engine.collision;

import org.joml.Vector3f;

public class Box extends CollisionShape {

    public Box() {
        super(getBoxCorners(), getAxes());
    }

    public Box(Vector3f min, Vector3f max) {
        super(getBoxCorners(min, max), getAxes());
    }

    private static Vector3f[] getAxes() {
        return new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)};
    }

    private static Vector3f[] getBoxCorners() {
        Vector3f[] cornerPoints = new Vector3f[8];
        int count = 0;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    cornerPoints[count++] = new Vector3f(x, y, z);
                }
            }
        }
        return cornerPoints;
    }

    private static Vector3f[] getBoxCorners(Vector3f min, Vector3f max) {
        Vector3f[] cornerPoints = new Vector3f[8];
        int count = 0;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    cornerPoints[count++] = new Vector3f(x == 0 ? min.x : max.x, y == 0 ? min.y : max.y, z == 0 ? min.z : max.z);
                }
            }
        }
        return cornerPoints;
    }
}
