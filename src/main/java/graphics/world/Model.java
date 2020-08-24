package graphics.world;

import engine.collision.Collider;
import graphics.objects.Vao;
import org.joml.Vector3f;

public class Model {

    public Vao mesh;
    public Collider collider;

    public Vector3f relativeCenter;
    public float height;
    public float radiusXZ;
    public float radius;

    public Model(Vao mesh,Vector3f relativeCenter, float height, float radiusXZ, float radius) {
        this.mesh = mesh;
        this.height = height;
        this.radius = radius;
        this.radiusXZ = radiusXZ;
        this.relativeCenter=relativeCenter;
    }

    public Model(Vao mesh,Vector3f relativeCenter, float height, float radiusXZ, float radius, Collider collider) {
        this(mesh,relativeCenter, height, radiusXZ, radius);
        this.collider = collider;

    }
}
