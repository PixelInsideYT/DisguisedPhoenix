package graphics.world;

import engine.collision.Collider;

public class Model {

    public Mesh[] meshes;
    public Collider collider;

    public Model(Mesh[] meshes) {
        this.meshes = meshes;
    }

    public Model(Mesh[] meshes, Collider collider) {
        this.meshes = meshes;
        this.collider = collider;
    }
}
