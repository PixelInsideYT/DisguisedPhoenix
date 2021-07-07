package engine.world;

import disuguisedphoenix.Entity;
import engine.util.Maths;
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
    private Vector3f centerPosition;
    private float halfWidth;
    private float halfHeight;
    private float halfDepth;
    private boolean hasChildren = false;
    private Octree[] nodes;
    private Vector3f min, max;

    private List<Entity> entities = new ArrayList<>();
    private Vector3f tempVec = new Vector3f();

    public Octree(Vector3f centerPosition, float width, float height, float depth) {
        this.centerPosition = centerPosition;
        this.halfWidth = width / 2f;
        this.halfHeight = height / 2f;
        this.halfDepth = depth / 2f;
        min = new Vector3f(centerPosition).sub(halfWidth, halfHeight, halfDepth);
        max = new Vector3f(centerPosition).add(halfWidth, halfHeight, halfDepth);
    }

    public static boolean couldBeVisible(Entity e, Vector3f cameraPos) {
        float size = e.getScale() * e.getModel().radius;
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
        List<Entity> toReinsert = new ArrayList<>();
        toReinsert.addAll(entities);
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

    public List<Entity> getAllVisibleEntities(FrustumIntersection frustum, Vector3f camPos) {
        if (frustum.testAab(min, max)) {
            if (hasChildren) {
                Stream<Entity> returnedStream = entities.stream();
                for (Octree node : nodes) {
                    returnedStream = Stream.concat(returnedStream, node.getAllVisibleEntities(frustum, camPos).stream());
                }
                return returnedStream.filter(e -> Maths.isInsideFrustum(frustum, e) && couldBeVisible(e, camPos)).collect(Collectors.toList());
            } else {
                return entities;
            }
        }
        return new ArrayList<>();
    }

    protected boolean contains(Entity e) {
        float entityRadius = e.getModel().radius * e.getScale();
        Vector3f relativeCenter = e.getModel().relativeCenter;
        Vector3f entityCenter = tempVec.set(e.getPosition())
                .add(relativeCenter.x * e.getScale(), relativeCenter.y * e.getScale(), relativeCenter.z * e.getScale());
        return Intersectionf.testAabSphere(min, max, entityCenter, entityRadius);
    }

    private boolean hasMinSize() {
        return halfWidth <= MIN_SIZE || halfHeight <= MIN_SIZE || halfDepth <= MIN_SIZE;
    }

    public Matrix4f getTransformationMatrix() {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix.translate(centerPosition);
        modelMatrix.scale(halfWidth - 0.01f, halfHeight - 0.01f, halfDepth - 0.01f);
        return modelMatrix;
    }

    public List<Matrix4f> getAllTransformationMatricies(int depth) {
        List<Matrix4f> list = new ArrayList<>();
        list.add(getTransformationMatrix());
        if (hasChildren && depth > 0) {
            for (Octree node : nodes) {
                list.addAll(node.getAllTransformationMatricies(depth - 1));
            }
        }
        return list;
    }

}
