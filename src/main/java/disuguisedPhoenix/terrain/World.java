package disuguisedPhoenix.terrain;

import disuguisedPhoenix.Entity;
import disuguisedPhoenix.EntityAdder;
import graphics.particles.ParticleManager;
import graphics.renderer.TestRenderer;
import graphics.world.Model;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class World {

    public List<PopulatedIsland> islands = new ArrayList<>();
    private EntityAdder adder;

    private Matrix4f cullingMatrix = new Matrix4f();
    private FrustumIntersection cullingHelper = new FrustumIntersection();

    private Map<Model, List<Entity>> modelEntityMap = new HashMap<>();

    public World(ParticleManager pm) {
        adder = new EntityAdder(pm);
    }


    public List<Entity> getPossibleCollisions(Entity e) {
        return islands.parallelStream().filter(island -> island.couldCollide(e)).flatMap(island -> island.getPopulation().stream().filter(p->p.getCollider()!=null)).collect(Collectors.toList());
    }

    public void addIsland(int bounds) {
        PopulatedIsland pi = new PopulatedIsland(generateVec(20000f), (float) Math.random() * 10000f + 1000);
        islands.add(pi.addEntities(adder.getAllEntities(pi)));
    }

    public void render(TestRenderer renderer, Matrix4f projMatrix, Matrix4f viewMatrix, Vector3f camPos) {
        cullingHelper.set(cullingMatrix.set(projMatrix).mul(viewMatrix));
        modelEntityMap.clear();
        modelEntityMap = islands.parallelStream()
                .filter(island -> island.isVisible(cullingHelper))
                .parallel().flatMap(island -> island.getVisiblePopulation(cullingHelper,camPos))
                .collect(Collectors.groupingBy(Entity::getModel));
        renderer.render(modelEntityMap);
        for (PopulatedIsland island : islands) {
            if (island.isVisible(cullingHelper)) {
                renderer.render(island.island.model, island.island.transformation);
            }
        }
    }

    private static Vector3f generateVec(float bounds) {
        return new Vector3f((float) (Math.random() * 2f - 1f) * bounds, (float) (Math.random() * 2f - 1f) * bounds, (float) (Math.random() * 2f - 1f) * bounds);
    }

}
