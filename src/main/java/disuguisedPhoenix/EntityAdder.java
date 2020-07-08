package disuguisedPhoenix;

import disuguisedPhoenix.terrain.Terrain;
import engine.util.BiMap;
import graphics.loader.ModelLoader;
import graphics.loader.TextureLoader;
import graphics.objects.Shader;
import graphics.objects.Vao;
import graphics.particles.ParticleEmitter;
import graphics.particles.ParticleManager;
import graphics.particles.PointSeekingEmitter;
import graphics.particles.UpwardsParticles;
import graphics.world.Mesh;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityAdder {

    private Shader creationShader;
    private int fontTextureId;

    private Model sphere;
    private Vector3f spherePosition;
    private float sphereSize;
    Matrix4f modelMatrix = new Matrix4f();

    private float growSpeed = 1000f;

    private float growSpeedPlants = 10f;

    private int activated = 0;
    private float time = 0;

    private Map<Entity, Float> entityWantedSize = new HashMap<>();
    private ParticleManager pm;
    private BiMap<Entity, ParticleEmitter> toReachEntities = new BiMap<>();
    private BiMap<Entity, ParticleEmitter> reachedEntities = new BiMap<>();

    public EntityAdder(ParticleManager pm) {
        creationShader = new Shader(Shader.loadShaderCode("creationVS"), Shader.loadShaderCode("creationFS")).combine();
        creationShader.bindAtrributs("pos", "normals")
                .loadUniforms("projMatrix", "viewMatrix", "transformationMatrix", "time", "textureMap");
        spherePosition = new Vector3f();
        sphere = ModelLoader.getModel("misc/sphere.obj");
        fontTextureId = TextureLoader.loadTexture("misc/uv.jpg");
        this.pm = pm;
    }

    public void update(float dt) {
        sphereSize += dt * growSpeed;
        time += dt;
        for (Entity e : entityWantedSize.keySet()) {
            if (toReachEntities.get(e).toRemove()){
                if(reachedEntities.get(e)==null){
                    float emitTime = entityWantedSize.get(e)/growSpeedPlants;
                    reachedEntities.put(e,new UpwardsParticles(new Vector3f(e.getPosition()),400,10,emitTime));
                    pm.addParticleEmitter(reachedEntities.get(e));
                }
                e.scale += dt * growSpeedPlants;
            }
        }
    }

    public void render(Matrix4f camMatrix, Matrix4f projMatrix) {
        if (entityWantedSize.keySet().size() > 0) {
            creationShader.bind();
            creationShader.loadMatrix("projMatrix", projMatrix);
            creationShader.loadMatrix("viewMatrix", camMatrix);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, fontTextureId);
            creationShader.loadFloat("time", time);
            modelMatrix.identity().translate(spherePosition).scale(sphereSize);
            //  render(sphere, modelMatrix);
            Map<Model, List<Matrix4f>> renderMap = new HashMap<>();
            entityWantedSize.keySet().forEach(e -> addEntity(e, renderMap));
            for (Model m : renderMap.keySet()) {
                render(m, renderMap.get(m).stream().toArray(Matrix4f[]::new));
            }
            creationShader.unbind();
        }
    }

    private static void addEntity(Entity entity, Map<Model, List<Matrix4f>> modelMap) {
        Model m = entity.getModel();
        if (modelMap.get(m) == null) {
            modelMap.put(m, new ArrayList<>());
        }
        modelMap.get(m).add(entity.getTransformationMatrix());
    }

    private void render(Model model, Matrix4f... modelMatrixArray) {
        for (Mesh m : model.meshes) {
            Vao toRender = m.mesh;
            toRender.bind();
            for (Matrix4f modelMatrix : modelMatrixArray) {
                creationShader.loadMatrix("transformationMatrix", modelMatrix);
                GL11.glDrawElements(GL11.GL_TRIANGLES, toRender.getIndiciesLength(), GL11.GL_UNSIGNED_INT, 0);
            }
            toRender.unbind();
        }
    }

    public List<Entity> getAddedEntities() {
        List<Entity> toReturn = new ArrayList<>();
        for (Entity e : entityWantedSize.keySet()) {
            if (e.scale >= entityWantedSize.get(e)) {
                e.scale = entityWantedSize.get(e);
                toReturn.add(e);
            }
        }
        toReturn.forEach(e -> {
            entityWantedSize.remove(e);
            toReachEntities.removeKey(e);
            reachedEntities.removeKey(e);
        });
        return toReturn;
    }

    public void generateNextEntities(Vector3f playerPos, Terrain terrain) {
        spherePosition.set(playerPos);
        sphereSize = 0;
        time = 0;
        List<Entity> newEntities = generateNextEntities(terrain);
        float particleLifeTime = 0.3f;
        int particlesCount = (int)(10000f/newEntities.size()/particleLifeTime);
        newEntities.forEach(e -> {
            entityWantedSize.put(e, e.scale);
            e.scale = 0;
            ParticleEmitter pe = new PointSeekingEmitter(playerPos, e.position, 700f, particlesCount,terrain);
            pm.addParticleEmitter(pe);
            toReachEntities.put(e, pe);
        });
    }

    private List<Entity> generateNextEntities(Terrain terrain) {
        switch (activated) {
            case 0:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/bendyTree.obj", "lowPolyTree/bendyTreeCollider.obj", 0f, 6f, 0f, 100)).collect(Collectors.toList());

            case 1:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/testTree.obj", "lowPolyTree/testTreeCollider.obj", 0, 6f, 0, 40)).collect(Collectors.toList());
            case 2:
                activated++;
                return IntStream.range(0, 250).mapToObj(i -> generateEntiy(terrain, "plants/flowerTest1.obj", "plants/flowerTest1Collider.obj", 0, 6f, 0, 20)).collect(Collectors.toList());
            case 3:
                activated++;
                return IntStream.range(0, 50).mapToObj(i -> generateEntiy(terrain, "lowPolyTree/tree2.obj", "lowPolyTree/tree2Collider.obj", 0, 6f, 0, 30)).collect(Collectors.toList());
            case 4:
                activated++;
                return IntStream.range(0, 500).mapToObj(i -> generateEntiy(terrain, "misc/rock.obj", "misc/rock.obj", 6f, 6f, 6f, 10)).collect(Collectors.toList());
            case 5:
                activated++;
                return IntStream.range(0, 10000).mapToObj(i -> generateEntiy(terrain, "plants/grass.obj", null, 0, 6f, 0, 10)).collect(Collectors.toList());

        }
        return null;
    }

    private Entity generateEntiy(Terrain terrain, String modelName, String collider, float rotRandomX, float rotRandomY, float rotRandomZ, float scale) {
        Random rnd = new Random();
        float x = rnd.nextFloat() * Terrain.SIZE;
        float z = rnd.nextFloat() * Terrain.SIZE;
        float h = terrain.getHeightOfTerrain(x, z);
        float scaleDiffrence = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        Entity e;
        if (collider != null) {
            e = new Entity(ModelLoader.getModel(modelName, collider), new Vector3f(x, h, z), rnd.nextFloat() * rotRandomX, rnd.nextFloat() * rotRandomY, rnd.nextFloat() * rotRandomZ, scale * scaleDiffrence);
        } else {
            e = new Entity(ModelLoader.getModel(modelName), new Vector3f(x, h, z), rnd.nextFloat() * rotRandomX, rnd.nextFloat() * rotRandomY, rnd.nextFloat() * rotRandomZ, scale * scaleDiffrence);
        }
        return e;
    }

}
