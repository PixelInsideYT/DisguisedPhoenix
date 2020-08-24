package engine.collision;

import graphics.objects.Vao;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Collider {

    public List<CollisionShape> allTheShapes = new ArrayList<>();
    public CollisionShape boundingBox;
    public Vao boundingBoxModel;

    public Collider cloneAndTransform(Matrix4f matrix) {
        Collider c = new Collider();
        c.boundingBox = this.boundingBox.cloneAndTransform(matrix);
        c.allTheShapes = this.allTheShapes.stream().map(cs -> cs.cloneAndTransform(matrix)).collect(Collectors.toList());
        c.boundingBoxModel = this.boundingBoxModel;
        return c;
    }

    public void transform(Matrix4f matrix) {
        boundingBox.transform(matrix);
        allTheShapes.forEach(cs -> cs.transform(matrix));
    }

    public void addCollisionShape(CollisionShape shape) {
        allTheShapes.add(shape);
    }

    public void setBoundingBox(Box b, boolean renderable) {
        this.boundingBox = b;
        if (renderable)
            boundingBoxModel = createModel(b);
    }

    private Vao createModel(Box b) {
        Vao vao = new Vao();
        int counter = 0;
        float[] data = new float[b.cornerPoints.length * 3];
        for (Vector3f v : b.cornerPoints) {
            data[counter++] = v.x;
            data[counter++] = v.y;
            data[counter++] = v.z;
        }
        vao.addDataAttributes(0, 3, data);
        int[] indicies = new int[]{0, 1, 2, 0, 2, 3,    // front
                4, 5, 6, 4, 6, 7,    // back
                8, 9, 10, 8, 10, 11,   // top
                12, 13, 14, 12, 14, 15,   // bottom
                16, 17, 18, 16, 18, 19,   // right
                20, 21, 22, 20, 22, 23,   // left
        };
        vao.addIndicies(indicies);
        return vao;
    }
}
