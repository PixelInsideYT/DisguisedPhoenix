package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Terrain;
import engine.util.BiMap;
import engine.util.ModelFileHandler;
import graphics.objects.Shader;
import graphics.objects.Vao;
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

    private float particlesPerSecondPerAreaUnit=0.0005f;
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

    public void update(float dt) {
        for (Entity e : entityBuiltProgress.keySet()) {
            if (toReachEntities.get(e).toRemove()) {
                if (reachedEntities.get(e) == null) {
                    float emitTime = 1f / builtSpeed;
                    float particleHeight = e.scale * e.getModel().height;
                    float radius = e.scale * e.getModel().radiusXZ;
                    reachedEntities.put(e, new UpwardsParticles(new Vector3f(e.getPosition()), radius, 500, 3.14f*radius*radius*particlesPerSecondPerAreaUnit, emitTime));
                    pm.addParticleEmitter(reachedEntities.get(e));
                } else {
                    reachedEntities.get(e).center.y += dt * builtSpeed * e.getModel().height * e.scale;
                }
                float newBuiltProgress = entityBuiltProgress.get(e).floatValue() + dt * builtSpeed;
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

    private static void addEntity(Entity entity, Map<Model, List<Entity>> modelMap) {
        Model m = entity.getModel();
        modelMap.computeIfAbsent(m, k -> new ArrayList<>());
        modelMap.get(m).add(entity);
    }

    private void render(Model model, Entity... toRenderEntities) {
        for (Vao toRender : model.meshes) {
            toRender.bind();
            creationShader.loadFloat("modelHeight", model.height);
            for (Entity e : toRenderEntities) {
                creationShader.loadMatrix("transformationMatrix", e.getTransformationMatrix());
                creationShader.loadFloat("builtProgress", entityBuiltProgress.get(e));
                GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
            }
            toRender.unbind();
        }
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

    public void generateNextEntities(Vector3f playerPos, Terrain terrain) {
        List<Entity> newEntities = generateNextEntities(terrain);
        float particleLifeTime = 0.3f;
        int particlesCount = (int) (10000f / newEntities.size() / particleLifeTime);
        newEntities.forEach(e -> {
            entityBuiltProgress.put(e, -0.01f);
            ParticleEmitter pe = new PointSeekingEmitter(playerPos, e.position, 700f, particlesCount, terrain);
            pm.addParticleEmitter(pe);
            toReachEntities.put(e, pe);
        });
    }

    private List<Entity> generateNextEntities(Terrain terrain) {
        switch (activated) {
            case 0:
                activated++;
                return IntStream.range(0, 250).mapToObj(i -> generateEntiy(terrain, "plants/flowerTest1.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 1:
                activated++;
                return IntStream.range(0, 20).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/vc.modelFile", 0f, 6f, 0f, 100)).collect(Collectors.toList());
            case 2:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/ballTree.modelFile", 0, 6f, 0, 40)).collect(Collectors.toList());
            case 3:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/bendyTree.modelFile", 0f, 6f, 0f, 100)).collect(Collectors.toList());
            case 4:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/tree2.modelFile", 0, 6f, 0, 30)).collect(Collectors.toList());
            case 5:
                activated++;
                return IntStream.range(0, 500).mapToObj(i -> generateEntiy(terrain, "misc/rock.modelFile", 6f, 6f, 6f, 10)).collect(Collectors.toList());
            case 6:
                activated++;
                return IntStream.range(0, 10000).mapToObj(i -> generateEntiy(terrain, "plants/grass.modelFile", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 7:
                activated++;
                return IntStream.range(0, 100).mapToObj(i -> generateEntiy(terrain, "plants/mushroom.modelFile", 0, 6f, 0, 10)).collect(Collectors.toList());
            case 8:
                activated++;
                return IntStream.range(0, 100).mapToObj(i -> generateEntiy(terrain, "misc/tutorialCrystal.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 9:
                activated++;
                return IntStream.range(0, 500).mapToObj(i -> generateEntiy(terrain, "plants/glockenblume.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());

        }
        return new ArrayList<>();
    }

    private Entity generateEntiy(Terrain terrain, String modelFile, float rotRandomX, float rotRandomY, float rotRandomZ, float scale) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * Terrain.SIZE;
        float z = rnd.nextFloat() * Terrain.SIZE;
        float h = terrain.getHeightOfTerrain(x, z);
        float scaleDiffrence = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        return new Entity(ModelFileHandler.getModel(modelFile), new Vector3f(x, h, z), rnd.nextFloat() * rotRandomX, rnd.nextFloat() * rotRandomY, rnd.nextFloat() * rotRandomZ, scale * scaleDiffrence);
    }

    public List<Entity> getAllEntities(Terrain terrain) {
        List<Entity> rt = new ArrayList<>();
        for (int i = 0; i < 10; i++)
            rt.addAll(generateNextEntities(terrain));
        return rt;
    }
}
