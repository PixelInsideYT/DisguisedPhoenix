package disuguisedPhoenix.adder;

import disuguisedPhoenix.Entity;
import disuguisedPhoenix.Main;
import disuguisedPhoenix.terrain.Island;
import disuguisedPhoenix.terrain.PopulatedIsland;
import disuguisedPhoenix.terrain.World;
import engine.util.Maths;
import engine.util.ModelFileHandler;
import graphics.shaders.Shader;
import graphics.particles.ParticleManager;
import graphics.shaders.ShaderFactory;
import graphics.world.Model;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL40;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EntityAdder {

    private final Shader creationShader;

    private final float particlesPerSecondPerAreaUnit = 0.0005f;
    private final float builtSpeed = 0.1f;

    private int activated = 0;

    private final ParticleManager pm;
    private List<GrowState> toAddEntities = new ArrayList<>();

    public EntityAdder(ParticleManager pm) {
        ShaderFactory creationFactory = new ShaderFactory("creationVS.glsl", "creationFS.glsl");
        creationFactory.addShaderStage(ShaderFactory.GEOMETRY_SHADER, "creationGS.glsl");
        creationFactory.withUniforms("projMatrix", "viewMatrix", "transformationMatrix", "builtProgress", "modelHeight");
        creationShader = creationFactory.withAttributes("pos", "color").built();
        this.pm = pm;
    }


    public void update(float dt) {
        Iterator<GrowState> itr = toAddEntities.iterator();
        while (itr.hasNext()) {
            GrowState gs = itr.next();
            gs.update(dt, builtSpeed, particlesPerSecondPerAreaUnit, pm);
            if (gs.isFullyGrown()) {
                gs.addToIsland();
                itr.remove();
            }
        }
    }

    public void render(Matrix4f camMatrix, Matrix4f projMatrix, FrustumIntersection fi) {
        if (toAddEntities.size() > 0) {
            creationShader.bind();
            creationShader.loadMatrix("projMatrix", projMatrix);
            creationShader.loadMatrix("viewMatrix", camMatrix);
            Map<Model, List<GrowState>> renderMap = new HashMap<>();
            toAddEntities.forEach(e -> addGrowStateToRenderMap(e, renderMap, fi));
            for (Model m : renderMap.keySet()) {
                render(m, renderMap.get(m));
            }
            creationShader.unbind();
        }
    }

    private void render(Model model, List<GrowState> toRenderEntities) {
        model.renderInfo.actualVao.bind();
        int indiciesLength = model.renderInfo.indiciesCount;
        creationShader.loadFloat("modelHeight", model.height);
        for (GrowState e : toRenderEntities) {
            creationShader.loadMatrix("transformationMatrix", e.growingEntity.getTransformationMatrix());
            creationShader.loadFloat("builtProgress", e.buildProgress);
            GL40.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, model.renderInfo.indexOffset * 4, model.renderInfo.vertexOffset);
            Main.inViewObjects++;
            Main.inViewVerticies += indiciesLength;
            Main.drawCalls++;
            Main.facesDrawn += indiciesLength / 3;
        }
        model.renderInfo.actualVao.unbind();
    }


    public void generateNextEntities(Vector3f playerPos, World world, List<PopulatedIsland> islands) {
        for (PopulatedIsland island : islands) {
            List<Entity> spawningEntities = generateEntitiesFor(island.island);
            for (Entity e : spawningEntities) {
                GrowState gr = new GrowState(world, island, 100, playerPos, e, pm);
                toAddEntities.add(gr);
            }
        }
        activated++;
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
            case 10:
                return IntStream.range(0, (int) (0.000001f * terrainAreaEstimate)).mapToObj(i -> generateEntiy(terrain, "misc/fox.modelFile", 0, 6f, 0, 20)).collect(Collectors.toList());

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

    public List<Entity> getAllEntities(PopulatedIsland flyingIslands) {
        List<Entity> rt = new ArrayList<>();
        activated = 0;
        for (int i = 0; i < 11; i++) {
            rt.addAll(generateEntitiesFor(flyingIslands.island));
            activated++;
        }
        return rt;
    }

    private static void addGrowStateToRenderMap(GrowState entity, Map<Model, List<GrowState>> modelMap, FrustumIntersection fi) {
        if (entity.isReachedBySeeker() && Maths.isInsideFrustum(fi, entity.growingEntity)) {
            Model m = entity.growingEntity.getModel();
            modelMap.computeIfAbsent(m, k -> new ArrayList<>());
            modelMap.get(m).add(entity);
        }
    }
}
