package graphics.world;

import engine.collision.Collider;
import graphics.objects.Vao;

public class Model {

    public Vao mesh;
    public Collider collider;

    public float height;
    public float radiusXZ;
    public float radius;

    public Model(Vao mesh, float height, float radiusXZ,float radius) {
        this.mesh = mesh;
        this.height = height;
        this.radius = radius;
        this.radiusXZ=radiusXZ;
    }

    public Model(Vao mesh, float height,float radiusXZ, float radius, Collider collider) {
        this(mesh, height, radiusXZ,radius);
        this.collider = collider;

    }
}
