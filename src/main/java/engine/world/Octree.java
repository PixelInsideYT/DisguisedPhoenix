package engine.world;

import disuguisedphoenix.Entity;
import org.joml.FrustumIntersection;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Octree {
    private static final float NEEDED_SIZE_PER_LENGTH_UNIT = 0.005f;

    private static final float MIN_SIZE = 2f;

    private int splitSize = 20;
    private final Vector3f centerPosition;
    private final float halfWidth;
    private final float halfHeight;
    private final float halfDepth;
    private boolean hasChildren = false;
    private Octree[] nodes;
    private final Vector3f min;
    private final Vector3f max;

    private final List<Entity> entities = new ArrayList<>();

    public Octree(Vector3f centerPosition, float width, float height, float depth) {
        this.centerPosition = centerPosition;
        this.halfWidth = width / 2f;
        this.halfHeight = height / 2f;
        this.halfDepth = depth / 2f;
        min = new Vector3f(centerPosition).sub(halfWidth, halfHeight, halfDepth);
        max = new Vector3f(centerPosition).add(halfWidth, halfHeight, halfDepth);
    }

    public static boolean couldBeVisible(Entity e, Vector3f cameraPos) {
        float size = e.getRadius();
        float distance = e.getPosition().distance(cameraPos);
        return size > distance * NEEDED_SIZE_PER_LENGTH_UNIT;
    }

    public void insert(Entity e) {
        int i = 0;
        int fittingCounter = 0;
        Octree fitting = null;
        while (hasChildren && i < nodes.length) {
            if (nodes[i].contains(e)) {
                fitting = nodes[i];
                fittingCounter++;
            }
            i++;
        }
        if (!hasChildren || fittingCounter != 1) {
            entities.add(e);
        } else {
            fitting.insert(e);
        }
        if (entities.size() > splitSize && !hasChildren && !hasMinSize()) {
            splitTree();
        }
    }

    private void splitTree() {
        hasChildren = true;
        nodes = new Octree[8];
        //cache all the entities to not end up in an endless loop
        List<Entity> toReinsert = new ArrayList<>(entities);
        entities.clear();
        float quarterWidth = halfWidth / 2f;
        float quarterHeight = halfHeight / 2f;
        float quarterDepth = halfDepth / 2f;
        for (int x = 0; x <= 1; x++) {
            float timesX = x * 2f - 1f;
            for (int y = 0; y <= 1; y++) {
                float timesY = y * 2f - 1f;
                for (int z = 0; z <= 1; z++) {
                    float timesZ = z * 2f - 1f;
                    nodes[x * 4 + y * 2 + z] = new Octree(new Vector3f(centerPosition).add(quarterWidth * timesX, quarterHeight * timesY, quarterDepth * timesZ), halfWidth, halfHeight, halfDepth);
                }
            }
        }
        //reinsert it in the this node and therefore leaves
        for (Entity e : toReinsert) {
            this.insert(e);
        }
    }

    private Stream.Builder<Entity> addVisibleEntitiesToBuilder(Stream.Builder<Entity> builder, FrustumIntersection frustum, Vector3f camPos) {
        if (frustum.testAab(min, max)) {
            entities.stream().filter(e->couldBeVisible(e,camPos)).forEach(builder::add);
            if (hasChildren) {
                for (Octree node : nodes) {
                    node.addVisibleEntitiesToBuilder(builder, frustum, camPos);
                }
            }
        }
        return builder;
    }

    private Stream.Builder<Octree> addVisibleNodesToBuilder(Stream.Builder<Octree> builder, FrustumIntersection frustum) {
        if (frustum.testAab(min, max)) {
            if(!entities.isEmpty()) {
                builder.add(this);
            }
            if (hasChildren) {
                for (Octree node : nodes) {
                    node.addVisibleNodesToBuilder(builder, frustum);
                }
            }
        }
        return builder;
    }

    public List<Octree> getAllVisibleNodes(FrustumIntersection frustum){
        return addVisibleNodesToBuilder(Stream.builder(),frustum).build().collect(Collectors.toList());
    }
/*
    public List<Entity> getAllVisibleEntities(FrustumIntersection frustum, Vector3f camPos) {
        return addVisibleEntitiesToBuilder(Stream.builder(),frustum, camPos).build().collect(Collectors.toList());
    }*/

    public List<Entity> getAllVisibleEntities(Vector3f camPos){
        return             entities.stream().filter(e->couldBeVisible(e,camPos)).collect(Collectors.toList());
    }

    protected boolean contains(Entity e) {
        return Intersectionf.testAabSphere(min, max, e.getCenter(), e.getRadius());
    }

    private boolean hasMinSize() {
        return halfWidth <= MIN_SIZE || halfHeight <= MIN_SIZE || halfDepth <= MIN_SIZE;
    }

    public Matrix4f getTransformationMatrix() {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.translate(centerPosition);
        modelMatrix.scale(halfWidth, halfHeight, halfDepth);
        return modelMatrix;
    }

    public List<Matrix4f> getAllTransformationMatrices(int depth) {
        List<Matrix4f> list = new ArrayList<>();
        list.add(getTransformationMatrix());
        if (hasChildren && depth > 0) {
            for (Octree node : nodes) {
                list.addAll(node.getAllTransformationMatrices(depth - 1));
            }
        }
        return list;
    }

    public Vector3f getMin(){
        return min;
    }

    public Vector3f getMax(){
        return max;
    }

    public Vector3f getCenter(){
        return centerPosition;
    }

    public List<Entity> getEntities(){
        return entities;
    }

}
