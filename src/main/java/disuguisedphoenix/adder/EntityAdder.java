package disuguisedphoenix.adder;

import com.google.gson.Gson;
import disuguisedphoenix.Entity;
import disuguisedphoenix.terrain.Island;
import engine.util.Maths;
import engine.util.ModelConfig;
import engine.util.ModelFileHandler;
import graphics.core.shaders.Shader;
import graphics.core.shaders.ShaderFactory;
import graphics.modelinfo.Model;
import graphics.particles.ParticleManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.InputStreamReader;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static disuguisedphoenix.Main.*;
import static org.lwjgl.opengl.GL40.glDrawElementsBaseVertex;

public class EntityAdder {

    private static final float PARTICLES_PER_SECOND_PER_AREA_UNIT = 0.0005f;
    private static final float BUILT_SPEED = 0.1f;
    private final Shader creationShader;
    private final ParticleManager pm;
    public List<String> modelNames = new ArrayList<>();
    Random rnd;
    private int activated = 0;
    private List<GrowState> toAddEntities = new ArrayList<>();
    private String[] exclude = new String[]{"birb", "lightPentagon", "cube","sphere"};

    public EntityAdder(ParticleManager pm) {
        ShaderFactory creationFactory = new ShaderFactory("creationVS.glsl", "creationFS.glsl");
        creationFactory.addShaderStage(ShaderFactory.GEOMETRY_SHADER, "creationGS.glsl");
        creationFactory.withUniforms("projMatrix", "viewMatrix", "transformationMatrix", "builtProgress", "modelHeight");
        creationShader = creationFactory.withAttributes("pos", "color").built();
        this.pm = pm;
        modelNames.addAll(getModelNameList().stream().filter(e -> {
            for (String ex : exclude) {
                if (e.contains(ex)) return false;
            }
            return true;
        }).collect(Collectors.toList()));
        rnd = new Random();
    }

    public static List<String> getModelNameList() {
        ModelConfig[] modelConfigs = new Gson().fromJson(new InputStreamReader(EntityAdder.class.getResourceAsStream("/models/ModelBuilder.info")), ModelConfig[].class);
        return Arrays.stream(modelConfigs)
                .map(modelConfig -> modelConfig.modelFilePath)
                .collect(Collectors.toList());
    }

    private static void addGrowStateToRenderMap(GrowState entity, Map<Model, List<GrowState>> modelMap, FrustumIntersection fi) {
        if (entity.isReachedBySeeker() && fi.testSphere(entity.growingEntity.getCenter(),entity.growingEntity.getRadius())) {
            Model m = entity.growingEntity.getModel();
            modelMap.computeIfAbsent(m, k -> new ArrayList<>());
            modelMap.get(m).add(entity);
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
            for (Map.Entry<Model, List<GrowState>> m : renderMap.entrySet()) {
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

    private List<Entity> generateEntitiesFor(float xRange, float xOffset, float yRange, float yOffset, float zRange, float zOffset, float terrainAreaEstimate, UnaryOperator<Vector3f> placementFunction) {
        if (activated < modelNames.size()) {
            Model model = ModelFileHandler.getModel(modelNames.get(activated));
            float modelAreaEstimate = (float) Math.PI * model.radiusXZ * model.radiusXZ*2f;
            float count = terrainAreaEstimate / modelAreaEstimate / modelNames.size();
            if (count > 100000) count = 100000;
            return IntStream.range(0, (int) count).mapToObj(i -> generateEntity(xRange, xOffset, yRange, yOffset, zRange, zOffset, placementFunction, model, 6f, 1f)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Entity generateEntity(float xRange, float xOffset, float yRange, float yOffset, float zRange, float zOffset, UnaryOperator<Vector3f> placementFunction, Model modelFile, float rotRandomZ, float scale) {
        Vector3f position = new Vector3f(rnd.nextFloat() * xRange + xOffset, rnd.nextFloat() * yRange + yOffset, rnd.nextFloat() * zRange + zOffset);
        float scaleDifference = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        return new Entity(modelFile, placementFunction.apply(position), 0, 0, rnd.nextFloat() * rotRandomZ, scale * scaleDifference);
    }

    public List<Entity> getAllEntities(Island island) {
        List<Entity> rt = new ArrayList<>();
        activated = 0;
        float areaEstimate = island.getSize() * island.getSize();
        for (int i = 0; i < modelNames.size(); i++) {
            rt.addAll(generateEntitiesFor(island.getSize(), 0, 0, 0, island.getSize(), 0, areaEstimate, island::placeVectorOnTerrain));
            activated++;
        }
        return rt;
    }

    public List<Entity> getAllEntities(float areaEstimate, UnaryOperator<Vector3f> placementFunction) {
        List<Entity> rt = new ArrayList<>();
        activated = 0;
        for (int i = 0; i < modelNames.size(); i++) {
            rt.addAll(generateEntitiesFor(2f, -1, 2f, -1f, 2f, -1f, areaEstimate, placementFunction));
            activated++;
        }
        return rt;
    }
}
