package engine.collision;

import graphics.objects.Vao;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Collider {

    List<CollisionShape> allTheShapes = new ArrayList<>();
    CollisionShape boundingBox;
    Vao boundingBoxModel;

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

    public void setBoundingBox(CollisionShape b) {
        this.boundingBox = b;
    }

    public List<CollisionShape> getAllTheShapes() {
        return allTheShapes;
    }

    public void setAllTheShapes(List<CollisionShape> allTheShapes) {
        this.allTheShapes = allTheShapes;
    }

    public CollisionShape getBoundingBox() {
        return boundingBox;
    }

    public Vao getBoundingBoxModel() {
        return boundingBoxModel;
    }

    public void setBoundingBoxModel(Vao boundingBoxModel) {
        this.boundingBoxModel = boundingBoxModel;
    }
}
