package de.thriemer.disguisedphoenix.terrain.generator;

import de.thriemer.graphics.loader.MeshInformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class TerrainGenerator {

    public MeshInformation buildTerrain(int subdivision, int index, UnaryOperator<Vector3f> heightMapper, BinaryOperator<Vector3f> colorMapper) {
        List<TerrainTriangle> sphereTriangles = new ArrayList<>();
        sphereTriangles.add(TerrainTriangle.getTriangle(index));
        for (int i = 0; i < subdivision; i++) {
            sphereTriangles = sphereTriangles.parallelStream().flatMap(TerrainTriangle::subdivide).collect(Collectors.toList());
        }
        List<Vector3f> sphereVerticies =
                sphereTriangles.stream().flatMap(t -> Arrays.stream(t.vecs))
                        .distinct()
                        .map(heightMapper)
                        .collect(Collectors.toList());
        int[] indices =
                sphereTriangles.stream().flatMap(t -> Arrays.stream(t.vecs)).mapToInt(sphereVerticies::indexOf).toArray();
        float[] v = new float[sphereVerticies.size() * 4];
        float[] color = new float[sphereVerticies.size() * 4];
        int[] counter = new int[sphereVerticies.size()];
        int pointer = 0;
        int colorPointer = 0;
        //color the triangles
        for (int i = 0; i < indices.length; i += 3) {
            int sphereIndex1 = indices[i];
            int sphereIndex2 = indices[i + 1];
            int sphereIndex3 = indices[i + 2];
            Vector3f v1 = sphereVerticies.get(sphereIndex1);
            Vector3f v2 = sphereVerticies.get(sphereIndex2);
            Vector3f v3 = sphereVerticies.get(sphereIndex3);
            Vector3f v1ToV2 = new Vector3f(v2).sub(v1);
            Vector3f v1ToV3 = new Vector3f(v3).sub(v1);
            Vector3f normal = v1ToV3.cross(v1ToV2).normalize();
            Vector3f avgVec = new Vector3f(v1).add(v2).add(v3).mul(1f / 3f);
            Vector3f calculatedColor = colorMapper.apply(avgVec, normal);
            addColorToArray(sphereIndex1, color, calculatedColor);
            addColorToArray(sphereIndex2, color, calculatedColor);
            addColorToArray(sphereIndex3, color, calculatedColor);
            counter[sphereIndex1]++;
            counter[sphereIndex2]++;
            counter[sphereIndex3]++;
        }
        for (Vector3f vec : sphereVerticies) {
            v[pointer++] = vec.x;
            v[pointer++] = vec.y;
            v[pointer++] = vec.z;
            v[pointer++] = 0f;
            float divideBy = counter[colorPointer / 4];
            color[colorPointer++] /= divideBy;
            color[colorPointer++] /= divideBy;
            color[colorPointer++] /= divideBy;
            color[colorPointer++] = 0;
        }
        return new MeshInformation(null,null,v,color,indices);
    }

    private void addColorToArray(int start, float[] array, Vector3f color) {
        array[start * 4] += color.x;
        array[start * 4 + 1] += color.y;
        array[start * 4 + 2] += color.z;
    }
}