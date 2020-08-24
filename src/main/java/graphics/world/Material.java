package graphics.world;

import org.joml.Vector3f;

public class Material {

    public static final Vector3f DEFAULT_COLOUR = new Vector3f(1, 1, 1);

    public String name;
    public Vector3f ambient;
    public Vector3f diffuse;
    public Vector3f specular;
    public float shininess = 0.0001f;
    public float opacity = 1f;
    public int diffuseTextureId;
    public int normalsTextureTd;
    public Material(String name) {
        this.name = name;
        ambient = new Vector3f(0.2f);
        diffuse = DEFAULT_COLOUR;
        specular = new Vector3f(0f);
    }
}
