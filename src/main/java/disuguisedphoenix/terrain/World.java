package disuguisedphoenix.terrain;

import disuguisedphoenix.Entity;
import disuguisedphoenix.adder.EntityAdder;
import disuguisedphoenix.terrain.generator.TerrainTriangle;
import disuguisedphoenix.terrain.generator.WorldGenerator;
import engine.world.Octree;
import graphics.core.objects.Vao;
import graphics.loader.MeshInformation;
import graphics.modelinfo.Model;
import graphics.modelinfo.RenderInfo;
import graphics.particles.ParticleManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class World {

    private final EntityAdder adder;
    private final FrustumIntersection cullingHelper = new FrustumIntersection();
    private final Octree staticEntities;
    private final List<Island> islands = new ArrayList<>();
    private final List<Terrain> terrains = new ArrayList<>();
    private final Matrix4f cullingMatrix = new Matrix4f();
    public static int addedEntities = 0;

    public World(ParticleManager pm, float worldSize) {
        adder = new EntityAdder(pm);
        staticEntities = new Octree(new Vector3f(0), worldSize, worldSize, worldSize);
    }

    public void update(float dt) {
        adder.update(dt);
    }

    public List<Entity> getPossibleCollisions(Entity e) {
        //TODO: implement oct tree for collision
        return new ArrayList<>();
    }
    Set<Integer> addedTerrains = new HashSet<>();

    List<Future<MeshInformation>> terrainFutures = new ArrayList<>();


    private ExecutorService executor = Executors.newFixedThreadPool(2);

    public void updatePlayerPos(Vector3f vector3f, WorldGenerator generator) {
        int terrainIndex = TerrainTriangle.getIndexForVector(vector3f);
        if (!addedTerrains.contains(terrainIndex)) {
            addedTerrains.add(terrainIndex);
            terrainFutures.add(executor.submit(() -> generator.createTerrainFor(vector3f)));
            PositionProvider positionProvider = new PositionProvider(TerrainTriangle.getTriangle(terrainIndex), generator::getNoiseFunction);
           executor.submit(()-> adder.getAllEntities(positionProvider.getArea(), positionProvider, generator::getNoiseFunction).forEach(this::placeUprightEntity));
        }
        Iterator<Future<MeshInformation>> itr = terrainFutures.iterator();
        while (itr.hasNext()) {
            Future<MeshInformation> singleMesh = itr.next();
            if (singleMesh != null && singleMesh.isDone()) {
                try {
                    MeshInformation terrainMesh = singleMesh.get();
                    Vao vao = new Vao();
                    vao.addDataAttributes(0, 4, terrainMesh.vertexPositions);
                    vao.addDataAttributes(1, 4, terrainMesh.colors);
                    vao.addIndicies(terrainMesh.indicies);
                    terrains.add(new Terrain(new Model(new RenderInfo(vao), null, 0, 0, 0, null)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                itr.remove();
            }
        }
    }


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

    public void placeUprightEntity(Entity e) {
        Vector3f eulerAngles = new Vector3f();
        Quaternionf qf = new Quaternionf();
        qf.rotateTo(new Vector3f(0, 1, 0), new Vector3f(e.getPosition()).normalize());
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

    public Model[] getTerrain() {
        return terrains.stream().map(Terrain::getModel).toArray(Model[]::new);
    }

    public void shutdown() {
        executor.shutdown();
    }

}
