package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Island;
import engine.util.BiMap;
import engine.util.ModelFileHandler;
import graphics.objects.Shader;
import graphics.particles.ParticleEmitter;
import graphics.particles.ParticleManager;
import graphics.particles.PointSeekingEmitter;
import graphics.particles.UpwardsParticles;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityAdder {

    private Shader creationShader;

    private float particlesPerSecondPerAreaUnit = 0.0005f;
    private float builtSpeed = 0.1f;

    private int activated = 0;

    private Map<Entity, Float> entityBuiltProgress = new HashMap<>();
    private ParticleManager pm;
    private BiMap<Entity, ParticleEmitter> toReachEntities = new BiMap<>();
    private BiMap<Entity, UpwardsParticles> reachedEntities = new BiMap<>();

    public EntityAdder(ParticleManager pm) {
        creationShader = new Shader(Shader.loadShaderCode("creationVS.glsl"), Shader.loadShaderCode("creationGS.glsl"), Shader.loadShaderCode("creationFS.glsl")).combine("pos", "color");
        creationShader.loadUniforms("projMatrix", "viewMatrix", "transformationMatrix", "builtProgress", "modelHeight");
        this.pm = pm;
    }

    private static void addEntity(Entity entity, Map<Model, List<Entity>> modelMap) {
        Model m = entity.getModel();
        modelMap.computeIfAbsent(m, k -> new ArrayList<>());
        modelMap.get(m).add(entity);
    }

    public void update(float dt) {
        for (Entity e : entityBuiltProgress.keySet()) {
            if (toReachEntities.get(e).toRemove()) {
                if (reachedEntities.get(e) == null) {
                    float emitTime = 1f / builtSpeed;
                    float particleHeight = e.scale * e.getModel().height;
                    float radius = e.scale * e.getModel().radiusXZ;
                    reachedEntities.put(e, new UpwardsParticles(new Vector3f(e.getPosition()), radius, 500, 3.14f * radius * radius * particlesPerSecondPerAreaUnit, emitTime));
                    pm.addParticleEmitter(reachedEntities.get(e));
                } else {
                    reachedEntities.get(e).center.y += dt * builtSpeed * e.getModel().height * e.scale;
                }
                float newBuiltProgress = entityBuiltProgress.get(e) + dt * builtSpeed;
                entityBuiltProgress.put(e, newBuiltProgress);
            }
        }
    }

    public void render(Matrix4f camMatrix, Matrix4f projMatrix) {
        if (entityBuiltProgress.keySet().size() > 0) {
            creationShader.bind();
            creationShader.loadMatrix("projMatrix", projMatrix);
            creationShader.loadMatrix("viewMatrix", camMatrix);
            Map<Model, List<Entity>> renderMap = new HashMap<>();
            entityBuiltProgress.keySet().forEach(e -> addEntity(e, renderMap));
            for (Model m : renderMap.keySet()) {
                render(m, renderMap.get(m).toArray(new Entity[0]));
            }
            creationShader.unbind();
        }
    }

    private void render(Model model, Entity... toRenderEntities) {
        model.mesh.bind();
        int indiciesLength = model.mesh.getIndiciesLength();
        creationShader.loadFloat("modelHeight", model.height);
        for (Entity e : toRenderEntities) {
            creationShader.loadMatrix("transformationMatrix", e.getTransformationMatrix());
            creationShader.loadFloat("builtProgress", entityBuiltProgress.get(e));
            GL11.glDrawElements(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, 0);
        }
        model.mesh.unbind();

    }

    public List<Entity> getAddedEntities() {
        List<Entity> toReturn = new ArrayList<>();
        for (Entity e : entityBuiltProgress.keySet()) {
            if (entityBuiltProgress.get(e) >= 1) {
                toReturn.add(e);
            }
        }
        toReturn.forEach(e -> {
            entityBuiltProgress.remove(e);
            toReachEntities.removeKey(e);
            reachedEntities.removeKey(e);
        });
        return toReturn;
    }

    public void generateNextEntities(Vector3f playerPos, List<Island> islands) {
        List<Entity> newEntities = new ArrayList<>();
        islands.forEach(i -> newEntities.addAll(generateEntitiesFor(i)));
        activated++;
        float particleLifeTime = 0.3f;
        int particlesCount = 100;
        newEntities.forEach(e -> {
            entityBuiltProgress.put(e, -0.01f);
            ParticleEmitter pe = new PointSeekingEmitter(playerPos, e.position, 700f, particlesCount, islands.get(0));
            pm.addParticleEmitter(pe);
            toReachEntities.put(e, pe);
        });
    }

    private List<Entity> generateEntitiesFor(Island terrain) {
        float terrainAreaEstimate = terrain.getSize() * terrain.getSize();
        switch (activated) {
            case 0:
                return IntStream.range(0, (int) (0.000005f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/flowerTest1.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 1:
                return IntStream.range(0, (int) (0.0000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/vc.modelFile", 0f, 6f, 0f, 100)).collect(Collectors.toList());
            case 2:
                return IntStream.range(0, (int) (0.0000005f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/ballTree.modelFile", 0, 6f, 0, 40)).collect(Collectors.toList());
            case 3:
                return IntStream.range(0, (int) (0.0000005f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/bendyTree.modelFile", 0f, 6f, 0f, 100)).collect(Collectors.toList());
            case 4:
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/tree2.modelFile", 0, 6f, 0, 30)).collect(Collectors.toList());
            case 5:
                return IntStream.range(0, (int) (0.00002f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "misc/rock.modelFile", 6f, 6f, 6f, 10)).collect(Collectors.toList());
            case 6:
                return IntStream.range(0, (int) (0.0002f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/grass.modelFile", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 7:
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/mushroom.modelFile", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 8:
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "misc/tutorialCrystal.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 9:
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/glockenblume.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());

        }
        return new ArrayList<>();
    }

    private List<Entity> generateNextEntities(Island terrain) {
        float terrainAreaEstimate = terrain.getSize() * terrain.getSize();
        switch (activated) {
            case 0:
                activated++;
                return IntStream.range(0, (int) (0.000005f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/flowerTest1.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 1:
                activated++;
                return IntStream.range(0, (int) (0.0000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/vc.modelFile", 0f, 6f, 0f, 100)).collect(Collectors.toList());
            case 2:
                activated++;
                return IntStream.range(0, (int) (0.0000005f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/ballTree.modelFile", 0, 6f, 0, 40)).collect(Collectors.toList());
            case 3:
                activated++;
                return IntStream.range(0, (int) (0.0000005f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/bendyTree.modelFile", 0f, 6f, 0f, 100)).collect(Collectors.toList());
            case 4:
                activated++;
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/tree2.modelFile", 0, 6f, 0, 30)).collect(Collectors.toList());
            case 5:
                activated++;
                return IntStream.range(0, (int) (0.00002f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "misc/rock.modelFile", 6f, 6f, 6f, 10)).collect(Collectors.toList());
            case 6:
                activated++;
                return IntStream.range(0, (int) (0.0002f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/grass.modelFile", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 7:
                activated++;
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/mushroom.modelFile", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 8:
                activated++;
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "misc/tutorialCrystal.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 9:
                activated++;
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "plants/glockenblume.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());

        }
        return new ArrayList<>();
    }

    private Entity generateEntiy(Island terrain, String modelFile, float rotRandomX, float rotRandomY, float rotRandomZ, float scale) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * terrain.getSize() + terrain.position.x;
        float z = rnd.nextFloat() * terrain.getSize() + terrain.position.z;
        float h = terrain.getHeightOfTerrain(x, terrain.position.y, z);
        float scaleDiffrence = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        return new Entity(ModelFileHandler.getModel(modelFile), new Vector3f(x, h, z), rnd.nextFloat() * rotRandomX, rnd.nextFloat() * rotRandomY, rnd.nextFloat() * rotRandomZ, scale * scaleDiffrence);
    }

    public List<Entity> getAllEntities(List<Island> flyingIslands) {
        List<Entity> rt = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (Island terrain : flyingIslands) {
                rt.addAll(generateEntitiesFor(terrain));
            }
            activated++;
        }
        return rt;
    }
}
