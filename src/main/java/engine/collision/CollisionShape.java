package engine.collision;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class CollisionShape {

    protected Vector3f[] cornerPoints;
    protected Vector3f center;
    protected Vector3f[] axes;

    public CollisionShape(Vector3f[] cornerPoints, Vector3f[] axes) {
        this.center = new Vector3f();
        this.cornerPoints = cornerPoints;
        this.axes = axes;
    }

    public CollisionShape cloneAndTransform(Matrix4f matrix) {
        CollisionShape newCs = this.clone();
        newCs.transform(matrix);
        return newCs;
    }

    public void transform(Matrix4f matrix) {
        for (int i = 0; i < cornerPoints.length; i++) {
            cornerPoints[i] = matrix.transformPosition(cornerPoints[i]);
        }
        for (int i = 0; i < axes.length; i++) {
            axes[i] = matrix.transformDirection(axes[i]).normalize();
        }
        center.add(matrix.getTranslation(new Vector3f()));
    }

    public Vector2f projectOnAxis(Vector3f axis) {
        float minProj = Float.MAX_VALUE;
        float maxProj = -Float.MAX_VALUE;
        for (Vector3f v : cornerPoints) {
            float dot = axis.dot(v);
            minProj = Math.min(dot, minProj);
            maxProj = Math.max(dot, maxProj);
        }
        return new Vector2f(minProj, maxProj);
    }

    public CollisionShape clone() {
        CollisionShape cs = new CollisionShape(cornerPoints.clone(), axes.clone());
        for (int i = 0; i < cs.cornerPoints.length; i++) {
            cs.cornerPoints[i] = new Vector3f(cs.cornerPoints[i]);
        }
        for (int i = 0; i < cs.axes.length; i++) {
            cs.axes[i] = new Vector3f(cs.axes[i]);
        }
        return cs;
    }

    public final Vector3f[] getAxis() {
        return axes;
    }

    public Vector3f getMax() {
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        for (Vector3f v : cornerPoints) {
            max.max(v);
        }
        return max;
    }

    public Vector3f getMin() {
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        for (Vector3f v : cornerPoints) {
            min.min(v);
        }
        return min;
    }

    public int getCornerPointCount() {
        return cornerPoints.length;
    }

    public final Vector3f[] getCornerPoints() {
        return cornerPoints;
    }

}
