package disuguisedphoenix.terrain;

import disuguisedphoenix.Entity;
import disuguisedphoenix.adder.EntityAdder;
import graphics.particles.ParticleManager;
import graphics.renderer.TestRenderer;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class World {

    public List<PopulatedIsland> islands = new ArrayList<>();
    private final EntityAdder adder;

    private final Matrix4f cullingMatrix = new Matrix4f();
    private final FrustumIntersection cullingHelper = new FrustumIntersection();

    public World(ParticleManager pm) {
        adder = new EntityAdder(pm);
    }

    public void update(float dt){
        adder.update(dt);
    }

    public List<Entity> getPossibleCollisions(Entity e) {
        return islands.parallelStream().filter(island -> island.couldCollide(e)).flatMap(island -> island.getPopulation().stream().filter(p -> p.getCollider() != null)).collect(Collectors.toList());
    }

    public void addIsland(int bounds) {
        PopulatedIsland pi = new PopulatedIsland(generateVec(bounds), (float) Math.random() * 10000f + 1000);
        islands.add(pi);
    }
    public void addIsland(PopulatedIsland i) {
        islands.add(i);
    }
    public void render(TestRenderer renderer, Matrix4f projMatrix, Matrix4f viewMatrix, Vector3f camPos) {
        cullingHelper.set(cullingMatrix.set(projMatrix).mul(viewMatrix));
        for (PopulatedIsland island : islands) {
            if (island.isVisible(cullingHelper)) {
                renderer.render(island.island.model, island.island.transformation);
            }
        }
    }

    public void renderAdder( Matrix4f projMatrix, Matrix4f viewMatrix){
        adder.render(viewMatrix,projMatrix,cullingHelper);
    }

    public void addNextEntities(Vector3f playerPos){
        adder.generateNextEntities(playerPos,this,islands);
    }

    public void addAllEntities(){
        islands.forEach(i->i.addEntities(adder.getAllEntities(i)));
    }

    public List<Entity> getVisibleEntities(Matrix4f projMatrix, Matrix4f viewMatrix, Vector3f camPos) {
        cullingHelper.set(cullingMatrix.set(projMatrix).mul(viewMatrix));
        return islands.parallelStream()
                .filter(island -> island.isVisible(cullingHelper))
                .parallel().flatMap(island -> island.getVisiblePopulation(cullingHelper, camPos)).collect(Collectors.toList());
    }
    public List<Entity> getAllEntities() {
        return islands.stream().flatMap(island -> island.getPopulation().stream()).collect(Collectors.toList());
    }
    private static Vector3f generateVec(float bounds) {
        return new Vector3f((float) (Math.random() * 2f - 1f) * bounds, (float) (Math.random() * 2f - 1f) * bounds, (float) (Math.random() * 2f - 1f) * bounds);
    }

    public List<Island> getPossibleTerrainCollisions(Entity e) {
        return getPossibleTerrainCollisions(e.getPosition());
    }

    public List<Island> getPossibleTerrainCollisions(Vector3f pos){
return islands.parallelStream().filter(island -> island.couldHeightCollide(pos)).map(populatedIsland -> populatedIsland.island).collect(Collectors.toList());
    }

}
