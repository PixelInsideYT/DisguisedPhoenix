package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.Main;
import de.thriemer.disguisedphoenix.Player;
import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.engine.time.CPUTimerQuery;
import de.thriemer.engine.util.Maths;
import de.thriemer.graphics.camera.Camera;
import de.thriemer.graphics.core.context.ContextInformation;
import de.thriemer.graphics.core.context.Display;
import de.thriemer.graphics.core.objects.*;
import de.thriemer.graphics.core.renderer.MultiIndirectRenderer;
import de.thriemer.graphics.core.renderer.TestRenderer;
import de.thriemer.graphics.modelinfo.Model;
import de.thriemer.graphics.modelinfo.RenderInfo;
import de.thriemer.graphics.particles.ParticleManager;
import de.thriemer.graphics.postprocessing.GaussianBlur;
import de.thriemer.graphics.postprocessing.HIZGenerator;
import de.thriemer.graphics.postprocessing.Pipeline;
import de.thriemer.graphics.postprocessing.QuadRenderer;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class MasterRenderer {

    public static final float NEAR_PLANE = 0.05f;
    public static final float FAR_PLANE =2000;
    private static final float FOV = 70;

    @Getter
    CameraInformation cameraInformation;

    GPUTimerQuery vertexTimer = new GPUTimerQuery("Geometry Pass");
    CPUTimerQuery entityCollectionTimer = new CPUTimerQuery("Entity Collection");

    private FrameBufferObject gBuffer;

    private VegetationRenderer vegetationRenderer;
    LightingPassRenderer lightingPassRenderer;
    private final OcclusionCalculator occlusionCalculator;
    private MultiIndirectRenderer multiIndirectRenderer;
    private QuadRenderer quadRenderer = new QuadRenderer();
    GaussianBlur blur = new GaussianBlur(quadRenderer);
    ShadowRenderer shadowRenderer;
    HIZGenerator hizGen = new HIZGenerator(quadRenderer);
    Pipeline postProcessPipeline;
    private final TestRenderer renderer;

    public MasterRenderer(ParticleManager particleManager,ContextInformation contextInformation) {
        int width = contextInformation.getWidth();
        int height = contextInformation.getHeight();
        cameraInformation = new CameraInformation(NEAR_PLANE, FAR_PLANE, FOV, contextInformation.getAspectRatio());
        multiIndirectRenderer = new MultiIndirectRenderer();
        vegetationRenderer = new VegetationRenderer(multiIndirectRenderer);
        renderer = new TestRenderer(vegetationRenderer.vegetationShader);
        lightingPassRenderer = new LightingPassRenderer(particleManager,quadRenderer, width, height);
        shadowRenderer = new ShadowRenderer(quadRenderer, contextInformation);
        setupFBOs(width, height);
        setupPostProcessing(width, height);
        occlusionCalculator = new OcclusionCalculator();
    }


    private void setupPostProcessing(int width, int height) {
        postProcessPipeline = new Pipeline(width, height, quadRenderer, blur);
    }

    private void setupFBOs(int width, int height) {
        gBuffer = new FrameBufferObject(width, height, 2)
                //normal and specular
                .addTextureAttachment(0)
                //color and geometry info
                .addTextureAttachment(1)
                //depth
                .addDepthTextureAttachment(true);
        gBuffer.unbind();
    }

    //TODO: deferred rendering with heck ton of lights

    public void render(boolean wireframe,Player player, Camera camera, Display display, Matrix4f viewMatrix, Vector3f camPos, float time, World world, Vector3f lightPos, Vector3f lightColor) {
       if(wireframe)
           OpenGLState.enableWireframe();
        cameraInformation.updateCameraMatrix(viewMatrix);
        entityCollectionTimer.startQuery();
        Map<Vao, Map<RenderInfo, List<Matrix4f>>> vaoSortedEntries = new HashMap<>();
        world.consumeVisibleEntities(cameraInformation.getProjViewMatrix(), (v,f) -> Maths.couldBeVisible(v,f, camPos), e -> consumeRenderEntity(e, vaoSortedEntries));
        entityCollectionTimer.stopQuery();
        vertexTimer.startQuery();
        OpenGLState.enableBackFaceCulling();
        OpenGLState.enableDepthTest();
        OpenGLState.disableAlphaBlending();
        gBuffer.bind();
        glClearColor(0.1f, 0.1f, 0.9f, 0.0f);
        gBuffer.clear();
        renderer.begin(cameraInformation);
        Matrix4f unitMatrix = new Matrix4f();
        for (Model model : world.getVisibleTerrains(cameraInformation.getProjViewMatrix())) {
            renderer.render(model, unitMatrix);
        }
        renderer.render(player.getModel(), player.getTransformationMatrix());
        hizGen.generateHiZMipMap(gBuffer);
        //TODO: more performant occlusion culling

        vegetationRenderer.prepareRender(vaoSortedEntries);
        gBuffer.bind();
        vegetationRenderer.render(time, cameraInformation);
        hizGen.generateHiZMipMap(gBuffer);
        vertexTimer.stopQuery();
        OpenGLState.disableWireframe();
        shadowRenderer.render(gBuffer, cameraInformation, time, lightPos, world, multiIndirectRenderer);
        OpenGLState.enableAlphaBlending();
        OpenGLState.disableDepthTest();
        gBuffer.blitDepth(lightingPassRenderer.deferredResult);
        lightingPassRenderer.render(gBuffer, shadowRenderer, cameraInformation, lightPos, lightColor);

        display.clear();
        postProcessPipeline.applyPostProcessing(display,cameraInformation.getProjectionMatrix(), lightingPassRenderer.deferredResult, gBuffer.getDepthTexture(), camera, shadowRenderer.shadowEffect.getShadowProjViewMatrix(), shadowRenderer.shadowEffect.getShadowTextureArray(), lightPos);
        // nuklearBinding.renderGUI(display.getWidth(),display.getHeight());
        display.flipBuffers();
    }
    //TODO: add GUI


    private void consumeRenderEntity(Entity e, Map<Vao, Map<RenderInfo, List<Matrix4f>>> vaoSortedEntries) {
        //sort entities according to Vao and model (remember one vao has multiple models)
        RenderInfo entityRenderInfo = e.getModel().getRenderInfo();
        if (entityRenderInfo.isMultiDrawCapable()) {
            Map<RenderInfo, List<Matrix4f>> instanceMap = vaoSortedEntries.computeIfAbsent(entityRenderInfo.getActualVao(), k -> new HashMap<>());
            List<Matrix4f> entityTransformation = instanceMap.computeIfAbsent(entityRenderInfo, k -> new ArrayList<>());
            entityTransformation.add(e.getTransformationMatrix());
            Main.inViewObjects++;
            Main.facesDrawn += entityRenderInfo.getIndicesCount() / 3;
        }
    }

    public void resize(ContextInformation contextInformation) {
        int width = contextInformation.getWidth();
        int height = contextInformation.getHeight();
        gBuffer.resize(width, height);
        lightingPassRenderer.deferredResult.resize(width, height);
        shadowRenderer.resize(contextInformation);
        postProcessPipeline.resize(width, height);
        cameraInformation.update(NEAR_PLANE, FAR_PLANE, FOV, contextInformation.getAspectRatio());
    }

    public BufferObject getMultiDrawVBO() {
        return multiIndirectRenderer.getPersistentMatrixVbo();
    }

}
