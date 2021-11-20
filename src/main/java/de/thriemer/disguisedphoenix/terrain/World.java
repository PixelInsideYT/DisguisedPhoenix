package de.thriemer.disguisedphoenix.terrain;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.adder.EntityAdder;
import de.thriemer.disguisedphoenix.rendering.CameraInformation;
import de.thriemer.disguisedphoenix.terrain.generator.TerrainTriangle;
import de.thriemer.disguisedphoenix.terrain.generator.WorldGenerator;
import de.thriemer.engine.world.Octree;
import de.thriemer.graphics.core.objects.Vao;
import de.thriemer.graphics.loader.MeshInformation;
import de.thriemer.graphics.modelinfo.Model;
import de.thriemer.graphics.modelinfo.RenderInfo;
import de.thriemer.graphics.particles.ParticleManager;
import lombok.Getter;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class World {

    private final EntityAdder adder;
    private final FrustumIntersection cullingHelper = new FrustumIntersection();
    @Getter
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


    private ExecutorService executor = Executors.newWorkStealingPool();


    public void updatePlayerPos(Vector3f vector3f, WorldGenerator generator) {
        Vector3f normalized = new Vector3f(vector3f).normalize();
        List<Integer> choosenTriangle = new ArrayList<>();
        ListIterator<TerrainTriangle> triangleIteratoritr = TerrainTriangle.getTriangleIterator();
        while (triangleIteratoritr.hasNext()) {
            int index = triangleIteratoritr.nextIndex();
            TerrainTriangle triangle = triangleIteratoritr.next();
            float dot = triangle.getDirection().normalize().dot(normalized);
            if (dot > 0.9) {
                choosenTriangle.add(index);
            }
        }
        for (int terrainIndex : choosenTriangle) {
            if (!addedTerrains.contains(terrainIndex)) {
                addedTerrains.add(terrainIndex);
                terrainFutures.add(executor.submit(() -> generator.createTerrainFor(terrainIndex)));
                PositionProvider positionProvider = new PositionProvider(TerrainTriangle.getTriangle(terrainIndex), generator::getNoiseFunction);
                executor.submit(() -> adder.getAllEntities(positionProvider.getArea(), positionProvider, generator::getNoiseFunction).forEach(this::placeUprightEntity));
            }
        }
        Iterator<Future<MeshInformation>> itr = terrainFutures.iterator();
        while (itr.hasNext()) {
            Future<MeshInformation> singleMesh = itr.next();
            if (singleMesh.isDone()) {
                try {
                    MeshInformation terrainMesh = singleMesh.get();
                    Vao vao = new Vao();
                    vao.addDataAttributes(0, 4, terrainMesh.vertexPositions);
                    vao.addDataAttributes(1, 4, terrainMesh.colors);
                    vao.addIndicies(terrainMesh.indicies);
                    terrains.add(new Terrain(new Model(new RenderInfo(vao), null, 0, 0, new Vector3f(), new Vector3f(), null)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                itr.remove();
            }
        }
    }

    public List<Entity> getVisibleEntities(Matrix4f projViewMatrix, Function<Entity, Boolean> visibilityFunction) {
        List<Entity> returnList = new LinkedList<>();
        consumeVisibleEntities(projViewMatrix,visibilityFunction,returnList::add);
        return returnList;
    }
    public void consumeVisibleEntities(Matrix4f projViewMatrix, Function<Entity, Boolean> visibilityFunction, Consumer<Entity> entityConsumer) {
        cullingHelper.set(projViewMatrix);
        staticEntities.getAllVisibleEntities(cullingHelper, visibilityFunction, entityConsumer);
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
        //  staticEntities.add(e);
        addedEntities++;
    }

    public Model[] getTerrain() {
        return terrains.stream().map(Terrain::getModel).toArray(Model[]::new);
    }

    public void shutdown() {
        executor.shutdown();
    }

}
