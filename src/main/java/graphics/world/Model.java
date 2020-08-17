package graphics.world;

import engine.collision.Collider;
import graphics.objects.Vao;

public class Model {

    public Vao[] meshes;
    public Collider collider;

    public float height;
    public float radiusXZ;

    public Model(Vao[] meshes, float height, float radius) {
        this.meshes = meshes;
        this.height = height;
        this.radiusXZ = radius;
    }

    public Model(Vao[] meshes, float height, float radius, Collider collider) {
        this(meshes, height, radius);
        this.collider = collider;

    }
}
