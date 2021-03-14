package disuguisedphoenix.adder;

import disuguisedphoenix.Entity;
import disuguisedphoenix.terrain.Island;
import disuguisedphoenix.terrain.PopulatedIsland;
import disuguisedphoenix.terrain.World;
import engine.util.Maths;
import engine.util.ModelFileHandler;
import graphics.particles.ParticleManager;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import graphics.world.Model;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static disuguisedphoenix.Main.*;
import static org.lwjgl.opengl.GL40.glDrawElementsBaseVertex;

public class EntityAdder {

    private final Shader creationShader;

    private static final float PARTICLES_PER_SECOND_PER_AREA_UNIT = 0.0005f;
    private static final float BUILT_SPEED = 0.1f;

    private int activated = 0;

    private List<String> modelNames = new ArrayList<>();

    private final ParticleManager pm;
    private List<GrowState> toAddEntities = new ArrayList<>();
    private String[] exclude = new String[]{"birb", "lightPentagon", "cube"};
    Random rnd;

    public EntityAdder(ParticleManager pm) {
        ShaderFactory creationFactory = new ShaderFactory("creationVS.glsl", "creationFS.glsl");
        creationFactory.addShaderStage(ShaderFactory.GEOMETRY_SHADER, "creationGS.glsl");
        creationFactory.withUniforms("projMatrix", "viewMatrix", "transformationMatrix", "builtProgress", "modelHeight");
        creationShader = creationFactory.withAttributes("pos", "color").built();
        this.pm = pm;
        modelNames.addAll(Arrays.asList("misc/tutorialCrystal.modelFile","misc/rock.modelFile","misc/fox.modelFile","lowPolyTree/tree2.modelFile","lowPolyTree/bendyTree.modelFile","lowPolyTree/vc.modelFile","lowPolyTree/ballTree.modelFile","lowPolyTree/bendyTreeCollider.modelFile","plants/glockenblume.modelFile","plants/flowerTest1.modelFile","plants/mushroom.modelFile","plants/grass.modelFile"));
        rnd = new Random();
    }

    public void fillModelNameList(File startDir) {
        File[] faFiles = startDir.listFiles();
        for (File file : faFiles) {
            if (file.getName().endsWith(".modelFile")) {
                boolean isBlocked = false;
                for (String s : exclude) {
                    if (file.getName().contains(s)) isBlocked = true;
                }
                String absPath = file.getAbsolutePath();
                String filename = absPath.substring(absPath.indexOf("models/") + "models/".length());
                if (!isBlocked) {
                    modelNames.add(filename);
                }
            }
            if (file.isDirectory()) {
                fillModelNameList(file);
            }
        }
    }

    public void update(float dt) {
        Iterator<GrowState> itr = toAddEntities.iterator();
        while (itr.hasNext()) {
            GrowState gs = itr.next();
            gs.update(dt, BUILT_SPEED, PARTICLES_PER_SECOND_PER_AREA_UNIT, pm);
            if (gs.isFullyGrown()) {
                gs.addToIsland();
                itr.remove();
            }
        }
    }

    public void render(Matrix4f camMatrix, Matrix4f projMatrix, FrustumIntersection fi) {
        if (!toAddEntities.isEmpty()) {
            creationShader.bind();
            creationShader.loadMatrix("projMatrix", projMatrix);
            creationShader.loadMatrix("viewMatrix", camMatrix);
            Map<Model, List<GrowState>> renderMap = new HashMap<>();
            toAddEntities.forEach(e -> addGrowStateToRenderMap(e, renderMap, fi));
            for (Map.Entry<Model,List<GrowState>> m : renderMap.entrySet()) {
                render(m.getKey(), m.getValue());
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
            glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indiciesLength, GL11.GL_UNSIGNED_INT, model.renderInfo.indexOffset * 4L, model.renderInfo.vertexOffset);
            inViewObjects++;
            inViewVerticies += indiciesLength;
            drawCalls++;
            facesDrawn += indiciesLength / 3;
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
        //float terrainAreaEstimate = terrain.getSize() * terrain.getSize();
        float terrainAreaEstimate = 4 * (float) Math.PI * scale * scale;
        if (activated < modelNames.size()) {
            Model model = ModelFileHandler.getModel(modelNames.get(activated));
            float modelAreaEstimate = (float) Math.PI * model.radiusXZ * model.radiusXZ;
            float count = terrainAreaEstimate / modelAreaEstimate / modelNames.size();
            if (count > 1000) count = 1000;
            return IntStream.range(0, (int) count).mapToObj(i -> generateEntiy(terrain, model, 0, 6f, 0, 1)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Entity generateEntiy(Island terrain, Model modelFile, float rotRandomX, float rotRandomY, float rotRandomZ, float scale) {

        float x = rnd.nextFloat() * terrain.getSize() + terrain.position.x;
        float z = rnd.nextFloat() * terrain.getSize() + terrain.position.z;
        float h = terrain.getHeightOfTerrain(x, terrain.position.y, z);
        float scaleDiffrence = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        return new Entity(modelFile, new Vector3f(x, h, z), rnd.nextFloat() * rotRandomX, rnd.nextFloat() * rotRandomY, rnd.nextFloat() * rotRandomZ, scale*scaleDiffrence);
    }

    public List<Entity> getAllEntities(PopulatedIsland flyingIslands) {
        List<Entity> rt = new ArrayList<>();
        activated = 0;
        for (int i = 0; i < modelNames.size(); i++) {
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
