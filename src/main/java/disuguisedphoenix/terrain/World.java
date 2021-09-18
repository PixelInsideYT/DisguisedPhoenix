package disuguisedphoenix.terrain;

import disuguisedphoenix.Entity;
import disuguisedphoenix.adder.EntityAdder;
import engine.world.Octree;
import graphics.particles.ParticleManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class World {

    private final EntityAdder adder;
    private final FrustumIntersection cullingHelper = new FrustumIntersection();
    private final Octree staticEntities;
    private final List<Island> islands = new ArrayList<>();
    private final Matrix4f cullingMatrix = new Matrix4f();
public static int addedEntities=0;
    public World(ParticleManager pm, float worldSize) {
        adder = new EntityAdder(pm);
        staticEntities = new Octree(new Vector3f(0), worldSize, worldSize, worldSize);
    }

    public void update(float dt) {
        adder.update(dt);
    }

    public List<Entity> getPossibleCollisions(Entity e) {
        //TODO: implement oct tree for collision
        return null;
    }

    /*
    public List<Entity> getVisibleEntities(Matrix4f projMatrix, Matrix4f viewMatrix, Vector3f camPos) {
        cullingHelper.set(cullingMatrix.set(projMatrix).mul(viewMatrix));
        return staticEntities.getAllVisibleEntities(cullingHelper, camPos);
    }
    */
    public List<Octree> getVisibleNodes(Matrix4f projMatrix, Matrix4f viewMatrix) {
        cullingHelper.set(cullingMatrix.set(projMatrix).mul(viewMatrix));
        return staticEntities.getAllVisibleNodes(cullingHelper);
    }
    public List<Island> getVisibleIslands() {
        return islands.stream().filter(i -> i.isVisible(cullingHelper)).collect(Collectors.toList());
    }

    public EntityAdder getEntityAdder() {
        return adder;
    }

    public void placeUprightEntity(Entity e, Vector3f position) {
        e.setPosition(position);
        Vector3f eulerAngles = new Vector3f();
        Quaternionf qf = new Quaternionf();
        qf.rotateTo(new Vector3f(0, 1, 0), new Vector3f(position).normalize());
        qf.mul(new Quaternionf().rotateLocalY(new Random().nextFloat() * 7f), qf);
        qf.getEulerAnglesXYZ(eulerAngles);
        e.setRotX(eulerAngles.x);
        e.setRotY(eulerAngles.y);
        e.setRotZ(eulerAngles.z);
        addEntity(e);
    }

    public void addEntity(Entity e) {
        staticEntities.insert(e);
        addedEntities++;
    }
}
