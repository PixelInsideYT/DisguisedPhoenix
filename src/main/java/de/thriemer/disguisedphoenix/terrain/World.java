package de.thriemer.disguisedphoenix.terrain;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.rendering.CameraInformation;
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
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.thriemer.disguisedphoenix.terrain.generator.TerrainGenerator.CHUNK_SIZE;

public class World {

    private final FrustumIntersection cullingHelper = new FrustumIntersection();
    @Getter
    private final Octree staticEntities;
    private final List<Terrain> terrains = new ArrayList<>();
    public static int addedEntities = 0;

    public World(ParticleManager pm, float worldSize) {
        staticEntities = new Octree(new Vector3f(0), worldSize, worldSize, worldSize);
    }

    public List<Entity> getPossibleCollisions(Entity e) {
        //TODO: implement oct tree for collision
        return new ArrayList<>();
    }

    Set<Vector3i> addedTerrains = new HashSet<>();

    List<Future<MeshInformation>> terrainFutures = new ArrayList<>();


    private ExecutorService executor = Executors.newWorkStealingPool();
    private int activeTasks =0;

    private List<Vector3i> getChunksInView(CameraInformation cameraInformation) {
        List<Vector3i> chunkList = new ArrayList<>();
        Vector3f frustumCornerMin = new Vector3f(Float.MAX_VALUE);
        Vector3f frustumCornerMax = new Vector3f(-Float.MAX_VALUE);
        for (int i = 0; i < 8; i++) {
            Vector3f corner = cameraInformation.getProjViewMatrix().frustumCorner(i, new Vector3f());
            frustumCornerMin.min(corner);
            frustumCornerMax.max(corner);
        }
        for (int x = (int) Math.floor(frustumCornerMin.x / CHUNK_SIZE); x < Math.ceil(frustumCornerMax.x / CHUNK_SIZE); x++) {
            for (int y = (int) Math.floor(frustumCornerMin.y / CHUNK_SIZE); y < Math.ceil(frustumCornerMax.y / CHUNK_SIZE); y++) {
                for (int z = (int) Math.floor(frustumCornerMin.z / CHUNK_SIZE); z < Math.ceil(frustumCornerMax.z / CHUNK_SIZE); z++) {
                    chunkList.add(new Vector3i(x, y, z));
                }
            }
        }
        Vector3f camPos =cameraInformation.getCameraPosition();
        Vector3i camPosInteger = new Vector3i((int)(camPos.x/CHUNK_SIZE),(int)(camPos.y/CHUNK_SIZE),(int)(camPos.z/CHUNK_SIZE));
        chunkList.sort(Comparator.comparingDouble(v -> v.distance(camPosInteger)));
        return chunkList;
    }

    public void updatePlayerPos(CameraInformation cameraInformation, WorldGenerator generator) {
        List<Vector3i> inViewChunks = getChunksInView(cameraInformation);
        int enqueued = 0;
        for (Vector3i terrainIndex : inViewChunks) {
            if (!addedTerrains.contains(terrainIndex)&&enqueued<5&&activeTasks<5) {
                addedTerrains.add(terrainIndex);
                terrainFutures.add(executor.submit(() -> generateChunk(generator, terrainIndex)));
                enqueued++;
                activeTasks++;
            }
        }
        Iterator<Future<MeshInformation>> itr = terrainFutures.iterator();
        while (itr.hasNext()) {
            Future<MeshInformation> singleMesh = itr.next();
            if (singleMesh.isDone()) {
                try {
                    MeshInformation terrainMesh = singleMesh.get();
                    if (terrainMesh.indicies.length > 0) {
                        Vao vao = new Vao();
                        vao.addDataAttributes(0, 4, terrainMesh.vertexPositions);
                        vao.addDataAttributes(1, 4, terrainMesh.colors);
                        vao.addIndicies(terrainMesh.indicies);
                        Model terrainModel = new Model(new RenderInfo(vao), terrainMesh);
                        terrains.add(new Terrain(terrainModel));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                activeTasks--;
                itr.remove();
            }
        }
    }

    private MeshInformation generateChunk(WorldGenerator generator, Vector3i terrainIndex) {
        MeshInformation generatedChunk = generator.createTerrainFor(terrainIndex);
        generator.addEntities(generatedChunk, this::addEntity);
        return generatedChunk;
    }

    public List<Entity> getVisibleEntities(Matrix4f projViewMatrix, Function<Entity, Boolean> visibilityFunction) {
        List<Entity> returnList = new LinkedList<>();
        consumeVisibleEntities(projViewMatrix, visibilityFunction, returnList::add);
        return returnList;
    }

    public void consumeVisibleEntities(Matrix4f projViewMatrix, Function<Entity, Boolean> visibilityFunction, Consumer<Entity> entityConsumer) {
        cullingHelper.set(projViewMatrix);
        staticEntities.getAllVisibleEntities(cullingHelper, visibilityFunction, entityConsumer);
    }

    public void addEntity(Entity e) {
        staticEntities.insert(e);
        //  staticEntities.add(e);
        addedEntities++;
    }

    public Model[] getVisibleTerrains() {
        return terrains.stream()
                .filter(t-> cullingHelper.testAab(t.model.getMinAABB(), t.getModel().getMaxAABB()))
                .map(Terrain::getModel).toArray(Model[]::new);
    }

    public void shutdown() {
        executor.shutdown();
    }

}
