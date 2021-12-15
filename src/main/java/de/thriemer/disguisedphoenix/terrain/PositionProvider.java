package de.thriemer.disguisedphoenix.terrain;

import de.thriemer.graphics.loader.MeshInformation;
import org.joml.Vector3f;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Random;

public class PositionProvider {

    Random random = new Random();
    MeshInformation meshInformation;

    public PositionProvider(MeshInformation meshInformation) {
        this.meshInformation = meshInformation;
    }

    public Map.Entry<Vector3f,Vector3f> getRandomPosition() {
        int randomFaceIndex = random.nextInt(meshInformation.getFaceCount());
        int index1 = meshInformation.indicies[randomFaceIndex * 3];
        int index2 = meshInformation.indicies[randomFaceIndex * 3 + 1];
        int index3 = meshInformation.indicies[randomFaceIndex * 3 + 2];
        Vector3f vec0 = meshInformation.getVector(index1);
        Vector3f vec1 = meshInformation.getVector(index2);
        Vector3f vec2 = meshInformation.getVector(index3);

        float bary1 =random.nextFloat();
        float bary2 = (1f-bary1)*random.nextFloat();
        Vector3f position = vec0
                .add(vec1.sub(vec0).mul(bary1))
                .add(vec2.sub(vec0).mul(bary2));
        Vector3f normal = vec1.cross(vec2).normalize();
       return new AbstractMap.SimpleEntry<>(position,normal);
    }

    public float getArea() {
        float area = 0;
        for (int i = 0; i < meshInformation.getFaceCount(); i++) {
            int index1 = meshInformation.indicies[i * 3];
            int index2 = meshInformation.indicies[i * 3 + 1];
            int index3 = meshInformation.indicies[i * 3 + 2];
            Vector3f vec0 = meshInformation.getVector(index1);
            Vector3f vec1 = meshInformation.getVector(index2);
            Vector3f vec2 = meshInformation.getVector(index3);
            area += getArea(vec0, vec1, vec2);
        }
        return area;
    }

    private float getArea(Vector3f v1, Vector3f v2, Vector3f v3) {
        return 1f / 2f * (v1.sub(v3)
                .cross(v2.sub(v3))
                .length());
    }

}
