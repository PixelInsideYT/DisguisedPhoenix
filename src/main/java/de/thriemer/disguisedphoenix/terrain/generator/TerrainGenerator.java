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
        int pointer = 0;
        int colorPointer = 0;
        //color the triangles
        for (int i = 0; i < sphereVerticies.size(); i ++) {
            Vector3f vec = sphereVerticies.get(i);
            Vector3f calculatedColor1 = colorMapper.apply(vec, new Vector3f());
            addColorToArray(i, color, calculatedColor1);
            v[pointer++] = vec.x;
            v[pointer++] = vec.y;
            v[pointer++] = vec.z;
            v[pointer++] = 0f;
        }
        return new MeshInformation(null,null,v,color,indices);
    }

    private void addColorToArray(int start, float[] array, Vector3f color) {
        array[start * 4] += color.x;
        array[start * 4 + 1] += color.y;
        array[start * 4 + 2] += color.z;
    }
}