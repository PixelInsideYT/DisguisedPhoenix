package disuguisedphoenix.terrain.generator;

import org.joml.Intersectionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TerrainTriangle {
   public Vector3f[] vecs;

    public TerrainTriangle(Vector3f... vecs) {
        this.vecs = vecs;
    }

    public TerrainTriangle(TerrainTriangle terrainTriangle){
        this(Arrays.stream(terrainTriangle.vecs).map(Vector3f::new).toArray(Vector3f[]::new));
    }

    private static final float PHI = (float) (1f + Math.sqrt(5)) / 2f;

    private static final int[][] FACES = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
    private static final Vector3f[] VERTS = {new Vector3f(-1, PHI, 0), new Vector3f(1, PHI, 0), new Vector3f(-1, -PHI, 0), new Vector3f(1, -PHI, 0), new Vector3f(0, -1, PHI), new Vector3f(0, 1, PHI), new Vector3f(0, -1, -PHI), new Vector3f(0, 1, -PHI), new Vector3f(PHI, 0, -1), new Vector3f(PHI, 0, 1), new Vector3f(-PHI, 0, -1), new Vector3f(-PHI, 0, 1)};
    private static List<TerrainTriangle> terrainTriangles = new ArrayList<>();
    static {
        for(int i=0;i<20;i++){
            terrainTriangles.addAll(getTriangle(FACES[i]).subdivide().flatMap(TerrainTriangle::subdivide).flatMap(TerrainTriangle::subdivide).collect(Collectors.toList()));
        }
    }

    public static ListIterator<TerrainTriangle> getTriangleIterator(){
        return terrainTriangles.listIterator();
    }

    private static TerrainTriangle getTriangle(int[] indices) {
        return new TerrainTriangle(new Vector3f(VERTS[indices[0]]),
                new Vector3f(VERTS[indices[1]]),
                new Vector3f(VERTS[indices[2]]));
    }

    public static TerrainTriangle getTriangle(int index) {
        return new TerrainTriangle(terrainTriangles.get(index));
    }

    public Stream<TerrainTriangle> subdivide() {
        Vector3f a = getMiddlePoint(vecs[0], vecs[1]);
        Vector3f b = getMiddlePoint(vecs[1], vecs[2]);
        Vector3f c = getMiddlePoint(vecs[2], vecs[0]);
        return Stream.of(
                new TerrainTriangle(a, b, c),
                new TerrainTriangle(vecs[0], a, c),
                new TerrainTriangle(vecs[1], b, a),
                new TerrainTriangle(vecs[2], c, b));
    }

    public Vector3f getNormal(){
        Vector3f ab = new Vector3f(vecs[0]).sub(vecs[1]);
        Vector3f ac = new Vector3f(vecs[0]).sub(vecs[2]);
        return ab.cross(ac);
    }

    public Vector3f getDirection(){
       return new Vector3f(vecs[0])
                .add(vecs[1]).add(vecs[2]).mul(1f/3f);
    }

    private static Vector3f getMiddlePoint(Vector3f v1, Vector3f v2) {
        return new Vector3f(v1).lerp(v2, 0.5f);
    }

    public static int getIndexForVector(Vector3f vector3f) {
        for (int i = 0; i < terrainTriangles.size(); i++) {
            Vector3f normalized = new Vector3f(vector3f).normalize();
            TerrainTriangle triangle = TerrainTriangle.getTriangle(i);
            if (Intersectionf.testRayTriangle(new Vector3f(),normalized,triangle.vecs[0],triangle.vecs[1],triangle.vecs[2],0.0001f)) {
                return i;
            }
        }
        return -1;
    }

}
