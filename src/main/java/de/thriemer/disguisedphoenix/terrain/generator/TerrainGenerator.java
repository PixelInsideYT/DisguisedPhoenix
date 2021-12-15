package de.thriemer.disguisedphoenix.terrain.generator;

import de.thriemer.graphics.loader.MeshInformation;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class TerrainGenerator {

    public static final float CHUNK_SIZE = 64;
    private static final float MESH_RESOLUTION = 2f;//every $MESH_RESOLUTION a vertex is placed
    private static final float ISO_LEVEL = 0.75f;

    private static final Vector3f[] CUBE_CORNERS = new Vector3f[]{
            new Vector3f(0, 0, 0),
            new Vector3f(1, 0, 0),
            new Vector3f(1, 0, 1),
            new Vector3f(0, 0, 1),
            new Vector3f(0, 1, 0),
            new Vector3f(1, 1, 0),
            new Vector3f(1, 1, 1),
            new Vector3f(0, 1, 1)
    };

    private static final Vector3f[] CUBE_EDGES = new Vector3f[]{
            //bot edges
            new Vector3f(CUBE_CORNERS[0]).add(CUBE_CORNERS[1]).div(2f),
            new Vector3f(CUBE_CORNERS[1]).add(CUBE_CORNERS[2]).div(2f),
            new Vector3f(CUBE_CORNERS[2]).add(CUBE_CORNERS[3]).div(2f),
            new Vector3f(CUBE_CORNERS[3]).add(CUBE_CORNERS[0]).div(2f),
            //top edges
            new Vector3f(CUBE_CORNERS[4]).add(CUBE_CORNERS[5]).div(2f),
            new Vector3f(CUBE_CORNERS[5]).add(CUBE_CORNERS[6]).div(2f),
            new Vector3f(CUBE_CORNERS[6]).add(CUBE_CORNERS[7]).div(2f),
            new Vector3f(CUBE_CORNERS[7]).add(CUBE_CORNERS[4]).div(2f),
            //vertical edges
            new Vector3f(CUBE_CORNERS[4]).add(CUBE_CORNERS[0]).div(2f),
            new Vector3f(CUBE_CORNERS[5]).add(CUBE_CORNERS[1]).div(2f),
            new Vector3f(CUBE_CORNERS[6]).add(CUBE_CORNERS[2]).div(2f),
            new Vector3f(CUBE_CORNERS[7]).add(CUBE_CORNERS[3]).div(2f)
    };


    public MeshInformation buildTerrain(Vector3i chunkIndex, Function<Vector3f, Float> noiseFunction, BinaryOperator<Vector3f> colorMapper) {
        List<Vector3f> triangleVertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int x = 0; x < CHUNK_SIZE / MESH_RESOLUTION; x++) {
            for (int y = 0; y < CHUNK_SIZE / MESH_RESOLUTION; y++) {
                for (int z = 0; z < CHUNK_SIZE / MESH_RESOLUTION; z++) {
                    //single cube
                    int configuration = 0;
                    for (int i = 0; i < 8; i++) {
                        Vector3f translated = transform(CUBE_CORNERS[i], chunkIndex, x, y, z);
                        float noiseValue = noiseFunction.apply(translated);
                        if (noiseValue > ISO_LEVEL) {
                            configuration |= 1 << i;
                        }
                    }
                    int[] cubeLocalIndices = MarchingCubesLookupTable.INDICES[configuration];
                    for (int i : cubeLocalIndices) {
                        Vector3f transformed = transform(CUBE_EDGES[i], chunkIndex, x, y, z);
                        if (!triangleVertices.contains(transformed)) {
                            triangleVertices.add(transformed);
                        }
                        indices.add(triangleVertices.indexOf(transformed));
                    }
                }
            }
        }
        float[] vertices = new float[triangleVertices.size() * 4];
        float[] colors = new float[triangleVertices.size() * 4];
        int[] indicesArray = indices.stream().mapToInt(i -> i).toArray();
        int index = 0;
        for (int i = 0; i < triangleVertices.size(); i++) {
            Vector3f vec = triangleVertices.get(i);
            Vector3f calculatedColor1 = colorMapper.apply(vec, new Vector3f());
            addColorToArray(i, colors, calculatedColor1);
            vertices[index++] = vec.x;
            vertices[index++] = vec.y;
            vertices[index++] = vec.z;
            vertices[index++] = 0f;
        }
        return new MeshInformation(chunkIndex.toString(), null, vertices, colors, indicesArray);
    }

    private void addColorToArray(int start, float[] array, Vector3f color) {
        array[start * 4] += color.x;
        array[start * 4 + 1] += color.y;
        array[start * 4 + 2] += color.z;
    }


    private Vector3f transform(Vector3f input, Vector3i chunkIndex, int x, int y, int z) {
        return new Vector3f(input).mul(MESH_RESOLUTION)
                .add(chunkIndex.x * CHUNK_SIZE + x * MESH_RESOLUTION,
                        chunkIndex.y * CHUNK_SIZE + y * MESH_RESOLUTION,
                        chunkIndex.z * CHUNK_SIZE + z * MESH_RESOLUTION);
    }

}