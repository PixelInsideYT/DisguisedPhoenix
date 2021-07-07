package graphics.loader;

import engine.collision.Collider;
import graphics.modelinfo.Material;
import org.joml.Vector3f;

public class MeshInformation {

    //TODO cleanup meshinfo, allow for DP intern format and ASSIMP format
    //TODO: make it more flexible

    public String meshName;
    public Material meshMaterial;

    public Collider collider;

    public float[] vertexPositions;
    public float[] colors;
    public int[] indicies;

    public float radiusXZPlane;
    public float height;
    public Vector3f centerPoint;
    public float radius;

    public MeshInformation(String meshName, Material material, float[] vertexPositions, float[] colors, int[] indicies) {
        this.meshName = meshName;
        this.meshMaterial = material;
        this.vertexPositions = vertexPositions;
        this.colors = colors;
        this.indicies = indicies;
        calculateHeightAndRadius();
    }

    public int getFaceCount() {
        return indicies.length / 3;
    }

    @Deprecated
    public int getVertexCount() {
        //TODO: make relevant again
        return vertexPositions.length / 3;
    }

    private void calculateHeightAndRadius() {
        height = radiusXZPlane = -Float.MAX_VALUE;
        for (int i = 0; i < getVertexCount(); i++) {
            Vector3f v = new Vector3f(vertexPositions[i], vertexPositions[i + 1], vertexPositions[i + 2]);
            height = Math.max(height, v.y);
            radiusXZPlane = Math.max(radiusXZPlane, (float) Math.sqrt(v.x * v.x + v.z * v.z));
        }
    }

}
