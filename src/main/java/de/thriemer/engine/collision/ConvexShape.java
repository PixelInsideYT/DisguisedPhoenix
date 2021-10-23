package de.thriemer.engine.collision;

import de.thriemer.graphics.core.objects.Vao;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ConvexShape extends CollisionShape {

    private final Matrix4f transformation;
    private Vao model;

    public ConvexShape(Vector3f[] cornerPoints, Vector3f[] axes) {
        super(cornerPoints, axes);
        transformation = new Matrix4f();
    }

    public ConvexShape(Vector3f[] cornerPoints, Vector3f[] axes, Vao model) {
        super(cornerPoints, axes);
        this.model = model;
        transformation = new Matrix4f();
    }

    @Override
    public ConvexShape clone() {
        Vector3f[] clonedCornerPoints = new Vector3f[this.cornerPoints.length];
        Vector3f[] clonedAxes = new Vector3f[this.axes.length];
        for (int i = 0; i < clonedCornerPoints.length; i++) {
            clonedCornerPoints[i] = new Vector3f(this.cornerPoints[i]);
        }
        for (int i = 0; i < axes.length; i++) {
            clonedAxes[i] = new Vector3f(this.axes[i]);
        }
        return new ConvexShape(clonedCornerPoints, clonedAxes, model);
    }

    @Override
    public void transform(Matrix4f matrix) {
        super.transform(matrix);
        matrix.mul(transformation, transformation);
    }

    public boolean canBeRenderd() {
        return model != null;
    }

    public final Matrix4f getTransformation() {
        return transformation;
    }

    public final Vao getModel() {
        return model;
    }

}
