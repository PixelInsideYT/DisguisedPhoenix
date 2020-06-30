package graphics.world;

import graphics.objects.Vao;

public class Mesh {
    public Vao mesh;
    public String materialName;

    public Mesh(Vao mesh, String materialIndex) {
        this.mesh = mesh;
        this.materialName = materialIndex;
    }
}
