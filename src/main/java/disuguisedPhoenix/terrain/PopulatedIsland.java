package disuguisedPhoenix.terrain;

import disuguisedPhoenix.Entity;
import engine.util.Maths;
import org.joml.FrustumIntersection;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PopulatedIsland {

    private static final float NEEDED_SIZE_PER_LENGTH_UNIT=0.005f;

    private Vector3f center;
    public Island island;
    public List<Entity> population = new ArrayList<>();
    public float radius;

    public PopulatedIsland(Vector3f position, float size) {
        island = new Island(position, size);
        this.center = new Vector3f(position).add(island.model.relativeCenter);
        this.radius = island.model.radius;
    }

    public boolean couldCollide(Entity e) {
        return new Vector3f(e.getModel().relativeCenter).add(e.getPosition()).distance(center) < e.getModel().radius + radius;
    }

    public void addEntity(Entity e) {
        float entityScale = e.getScale();
        Vector3f entityModelMiddlePoint = e.getModel().relativeCenter;
        Vector3f entityCenter = new Vector3f(e.getPosition());
        entityCenter.add(entityModelMiddlePoint.x * entityScale, entityModelMiddlePoint.y * entityScale, entityModelMiddlePoint.z * entityScale);
        float possibleNewRadius = entityCenter.distance(center) + e.getScale() * e.getModel().radius;
        if (possibleNewRadius != Float.POSITIVE_INFINITY) {
            radius = Math.max(radius, possibleNewRadius);
            population.add(e);
        }
    }

    public boolean isVisible(FrustumIntersection fi) {
        return Maths.isInsideFrustum(fi, island.position, island.model.relativeCenter, 1f, radius);
    }

    public Stream<Entity> getVisiblePopulation(FrustumIntersection fi,Vector3f cameraPos) {
        return population.stream().filter(e -> Maths.isInsideFrustum(fi, e.getPosition(), e.getModel().relativeCenter, e.getModel().radius, e.getScale())&&couldBeVisible(e,cameraPos));
    }

    public List<Entity> getPopulation() {
        return population;
    }

    public PopulatedIsland addEntities(List<Entity> addedEntities) {
        addedEntities.forEach(this::addEntity);
        return this;
    }

    private boolean couldBeVisible(Entity e, Vector3f cameraPos){
        float size = e.getScale()*e.getModel().radius;
        float distance = e.getPosition().distance(cameraPos);
        return size>distance*NEEDED_SIZE_PER_LENGTH_UNIT;
    }

}
