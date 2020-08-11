package graphics.world;

import engine.collision.Collider;
import graphics.objects.Vao;

public class Model {

    public Vao[] meshes;
    public Collider collider;

    public Model(Vao[] meshes) {
        this.meshes = meshes;
    }

    public Model(Vao[] meshes, Collider collider) {
        this.meshes = meshes;
        this.collider = collider;
    }
}
