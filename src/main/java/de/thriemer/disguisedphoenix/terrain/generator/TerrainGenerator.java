package de.thriemer.disguisedphoenix.terrain.generator;

import de.thriemer.graphics.loader.MeshInformation;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.spongepowered.noise.module.source.RidgedMultiSimplex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class TerrainGenerator {

    public static final float CHUNK_SIZE = 64;
    private static final float MESH_RESOLUTION = 8f;//every $MESH_RESOLUTION a vertex is placed
    private static final float ISO_LEVEL = 0.8f;

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


    public MeshInformation buildTerrain(Vector3i chunkIndex, Function<Vector3f, Float> noiseFunction, BinaryOperator<Vector3f> colorMapper) {
        List<Vector3f> triangleVertices = new ArrayList<>();
        Map<Vector3f, Float> cachedNoiseFunction = new HashMap<>();
        List<Integer> indices = new ArrayList<>();
        for (int x = 0; x < CHUNK_SIZE / MESH_RESOLUTION; x++) {
            for (int y = 0; y < CHUNK_SIZE / MESH_RESOLUTION; y++) {
                for (int z = 0; z < CHUNK_SIZE / MESH_RESOLUTION; z++) {
                    //single cube
                    int configuration = 0;
                    float[] noiseValues = new float[8];
                    Vector3f[] transformedVecs = new Vector3f[8];
                    for (int i = 0; i < 8; i++) {
                        Vector3f translated = transform(CUBE_CORNERS[i], chunkIndex, x, y, z);
                        transformedVecs[i] = translated;
                        float noiseValue = cachedNoiseFunction.computeIfAbsent(translated, noiseFunction);
                        noiseValues[i] = noiseValue;
                        if (noiseValue > ISO_LEVEL) {
                            configuration |= 1 << i;
                        }
                    }
                    int[] cubeLocalIndices = MarchingCubesLookupTable.INDICES[configuration];
                    for (int i : cubeLocalIndices) {
                        Vector3f transformed = transform(transformedVecs, noiseValues, MarchingCubesLookupTable.CUBE_EDGES[i]);
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

    private Vector3f transform(Vector3f[] vecs, float[] values, int[] edge) {
        int li = edge[0];
        int ri = edge[1];
        float a = (ISO_LEVEL - values[li]) / (values[ri] - values[li]);
        return new Vector3f(vecs[ri]).sub(vecs[li]).mul(a).add(vecs[li]);
    }

}