package de.thriemer.disguisedphoenix.adder;

import com.google.gson.Gson;
import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.terrain.PositionProvider;
import de.thriemer.engine.util.ModelConfig;
import de.thriemer.engine.util.ModelFileHandler;
import de.thriemer.graphics.core.objects.Vao;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import de.thriemer.graphics.modelinfo.Model;
import de.thriemer.graphics.modelinfo.RenderInfo;
import de.thriemer.graphics.particles.ParticleManager;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.InputStreamReader;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.thriemer.disguisedphoenix.Main.*;
import static org.lwjgl.opengl.GL40.glDrawElementsBaseVertex;

public class EntityAdder {

    //TODO: rework entity adding, support biomes, easier placement
    private static final float PARTICLES_PER_SECOND_PER_AREA_UNIT = 0.0005f;
    private static final float BUILT_SPEED = 0.1f;
    private final Shader creationShader;
    private final ParticleManager pm;
    public List<String> modelNames = new ArrayList<>();
    Random rnd;
    private int activated = 0;
    private final List<GrowState> toAddEntities = new ArrayList<>();
    private final String[] exclude = new String[]{"birb", "lightPentagon", "cube","sphere"};

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
        RenderInfo renderInfo = model.getRenderInfo();
        Vao actualVao = renderInfo.getActualVao();
        actualVao.bind();
        int indicesLength = model.getRenderInfo().getIndicesCount();
       //TODO: Modelheight
        // creationShader.loadFloat("modelHeight", model.getHeight());
        for (GrowState e : toRenderEntities) {
            creationShader.loadMatrix("transformationMatrix", e.growingEntity.getTransformationMatrix());
            creationShader.loadFloat("builtProgress", e.buildProgress);
            glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indicesLength, GL11.GL_UNSIGNED_INT,
                    renderInfo.getIndexOffset() * 4L, renderInfo.getVertexOffset());
            inViewObjects++;
            inViewVerticies += indicesLength;
            drawCalls++;
            facesDrawn += indicesLength / 3;
        }
        actualVao.unbind();
    }

    private List<Entity> generateEntitiesFor(float terrainAreaEstimate,PositionProvider positionProvider, UnaryOperator<Vector3f> placementFunction) {
        if (activated < modelNames.size()) {
            Model model = ModelFileHandler.getModel(modelNames.get(activated));
            float modelAreaEstimate = (float) Math.PI * model.getRadiusXZ() * model.getRadiusXZ()*2f;
            float count = terrainAreaEstimate / modelAreaEstimate / modelNames.size();
            return IntStream.range(0, (int) count).mapToObj(i -> generateEntity( placementFunction,positionProvider, model)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Entity generateEntity(UnaryOperator<Vector3f> placementFunction, PositionProvider positionProvider, Model modelFile) {
        Vector3f position = positionProvider.getRandomPosition();
        float scaleDifference = (rnd.nextFloat() * 2f - 1) * 0.5f + 1.0f;
        return new Entity(modelFile, placementFunction.apply(position), 0, 0, rnd.nextFloat() * 7f, scaleDifference);
    }


    public List<Entity> getAllEntities(float areaEstimate,PositionProvider positionProvider, UnaryOperator<Vector3f> placementFunction) {
        List<Entity> rt = new ArrayList<>();
        activated = 0;
        for (int i = 0; i < modelNames.size(); i++) {
            rt.addAll(generateEntitiesFor(areaEstimate,positionProvider, placementFunction));
            activated++;
        }
        return rt;
    }
}
