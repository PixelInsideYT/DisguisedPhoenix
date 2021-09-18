package disuguisedphoenix.terrain.generator;

import disuguisedphoenix.Main;
import engine.collision.CollisionShape;
import graphics.core.objects.Vao;
import graphics.modelinfo.Model;
import graphics.modelinfo.RenderInfo;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class TerrainGenerator {


    public Model createSphere(Consumer<Vector3f> heightMapper, BiFunction<Vector3f,Vector3f,Vector3f> colorMapper) {
        float scale = 100;
        int subdivisions = 400;
        List<Vector3f> sphereVerticies = createPlane(subdivisions, scale, 1, 0, 0);
        int indicyOffset = sphereVerticies.size();
        sphereVerticies.addAll(createPlane(subdivisions, scale, -1, 0, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, -1, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 1, 0));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 0, 1));
        sphereVerticies.addAll(createPlane(subdivisions, scale, 0, 0, -1));
        sphereVerticies.forEach(heightMapper);
        List<Integer> sphereIndicies = createIndiecies(subdivisions, 0, true);
        for (int i = 1; i < 6; i++) {
            sphereIndicies.addAll(createIndiecies(subdivisions, indicyOffset * i, i % 2 == 0));
        }
        float[] v = new float[sphereVerticies.size() * 4];
        float[] color = new float[sphereVerticies.size() * 4];
        int[] counter = new int[sphereVerticies.size()];
        int pointer = 0;
        int colorPointer = 0;
        //create world triangle collisions
        for (int i = 0; i < sphereIndicies.size(); i += 3) {
            int sphereIndex1 = sphereIndicies.get(i);
            int sphereIndex2 = sphereIndicies.get(i+1);
            int sphereIndex3 = sphereIndicies.get(i+2);
            Vector3f v1 = sphereVerticies.get(sphereIndex1);
            Vector3f v2 = sphereVerticies.get(sphereIndex2);
            Vector3f v3 = sphereVerticies.get(sphereIndex3);
            Vector3f v1ToV2 = new Vector3f(v2).sub(v1);
            Vector3f v1ToV3 = new Vector3f(v3).sub(v1);
            Vector3f normal = v1ToV3.cross(v1ToV2).normalize();
            Vector3f avgVec = new Vector3f(v1).add(v2).add(v3).mul(1f/3f);
            Vector3f calculatedColor = colorMapper.apply(avgVec,normal);
            addColorToArray(sphereIndex1,color,calculatedColor);
            addColorToArray(sphereIndex2,color,calculatedColor);
            addColorToArray(sphereIndex3,color,calculatedColor);
            counter[sphereIndex1]++;
            counter[sphereIndex2]++;
            counter[sphereIndex3]++;
        }
        for (Vector3f vec : sphereVerticies) {
            v[pointer++] = vec.x;
            v[pointer++] = vec.y;
            v[pointer++] = vec.z;
            v[pointer++] = 0f;
            float divideBy = counter[colorPointer/4];
            color[colorPointer++] /=divideBy;
            color[colorPointer++] /=divideBy;
            color[colorPointer++] /=divideBy;
            color[colorPointer++] = 0;
        }
        int[] plane1Indicies = sphereIndicies.stream().mapToInt(i -> i).toArray();
        Vao vao = new Vao();
        vao.addDataAttributes(0, 4, v);
        vao.addDataAttributes(1, 4, color);
        vao.addIndicies(plane1Indicies);
        return new Model(new RenderInfo(vao), null, 0, 0, 0, null);
    }


    private List<Vector3f> createPlane(int supdivisions, float scale, int xz, int yz, int xy) {
        List<Vector3f> rt = new ArrayList<>();
        float half = scale / 2f;
        float unit = 1f / (1f + supdivisions);
        for (int x = 0; x < 2 + supdivisions; x++) {
            for (int y = 0; y < 2 + supdivisions; y++) {
                float xx, yy, zz;
                xx = yy = zz = 0;
                //xz
                xx += (x * unit * scale - half) * xz;
                yy += half * xz;
                zz += (y * unit * scale - half) * xz;
                //yz
                yy += (x * unit * scale - half) * yz;
                xx += half * yz;
                zz += (y * unit * scale - half) * yz;
                //xy
                yy += (x * unit * scale - half) * xy;
                zz += half * xy;
                xx += (y * unit * scale - half) * xy;

                Vector3f v = new Vector3f(xx, yy, zz);
                rt.add(v);
            }
        }
        return rt;
    }

    private List<Integer> createIndiecies(int subdivisions, int offset, boolean flip) {
        List<Integer> indicies = new ArrayList<>();
        int vertexCount = 2 + subdivisions;
        for (int z = 0; z < 2 + subdivisions - 1; z++) {
            for (int x = 0; x < 2 + subdivisions - 1; x++) {
                int topLeft = x + z * vertexCount + offset;
                int topRight = topLeft + 1;
                int botLeft = topLeft + vertexCount;
                int botRight = topRight + vertexCount;
                if (flip) indicies.add(botLeft);
                indicies.add(topLeft);
                if (!flip) indicies.add(botLeft);
                indicies.add(topRight);
                if (flip) indicies.add(botLeft);
                indicies.add(topRight);
                if (!flip) indicies.add(botLeft);
                indicies.add(botRight);
            }
        }
        return indicies;
    }

    private void addColorToArray(int start, float[] array, Vector3f color){
        array[start*4]+=color.x;
        array[start*4+1]+=color.y;
        array[start*4+2]+=color.z;
    }

}
