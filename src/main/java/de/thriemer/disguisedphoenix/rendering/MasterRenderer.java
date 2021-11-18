package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.Player;
import de.thriemer.disguisedphoenix.terrain.Island;
import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.engine.time.CPUTimerQuery;
import de.thriemer.engine.util.Maths;
import de.thriemer.graphics.core.context.Display;
import de.thriemer.graphics.core.objects.BufferObject;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.objects.GPUTimerQuery;
import de.thriemer.graphics.core.objects.OpenGLState;
import de.thriemer.graphics.core.renderer.MultiIndirectRenderer;
import de.thriemer.graphics.core.renderer.TestRenderer;
import de.thriemer.graphics.modelinfo.Model;
import de.thriemer.graphics.postprocessing.GaussianBlur;
import de.thriemer.graphics.postprocessing.HIZGenerator;
import de.thriemer.graphics.postprocessing.Pipeline;
import de.thriemer.graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class MasterRenderer {

    public static final float FAR_PLANE = 1000f;
    public static final float NEAR_PLANE = 0.05f;
    public static final float FOV = 70;

    private int width;
    private int height;
    private float aspectRatio;
    GPUTimerQuery vertexTimer = new GPUTimerQuery("Geometry Pass");
    CPUTimerQuery entityCollectionTimer = new CPUTimerQuery("Entity Collection");

    Matrix4f projMatrix = new Matrix4f();

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

    public MasterRenderer(int width, int height, float aspectRatio){
        this.width=width;
        this.height=height;
        this.aspectRatio=aspectRatio;
        setProjMatrix();
        multiIndirectRenderer = new MultiIndirectRenderer();
        vegetationRenderer = new VegetationRenderer(multiIndirectRenderer);
        renderer = new TestRenderer(vegetationRenderer.vegetationShader);
        lightingPassRenderer = new LightingPassRenderer(quadRenderer,width,height);
        shadowRenderer = new ShadowRenderer(quadRenderer,width,height);
        setupFBOs(width,height);
        setupPostProcessing(width,height,projMatrix);
        occlusionCalculator = new OcclusionCalculator();
    }


    private void setupPostProcessing(int width, int height, Matrix4f projMatrix) {
        postProcessPipeline = new Pipeline(width, height, projMatrix, quadRenderer, blur);
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

    public void render(Player player, Display display, Matrix4f viewMatrix, Vector3f camPos, float time, World world, Vector3f lightPos, Vector3f lightColor) {
        vertexTimer.startQuery();
        OpenGLState.enableBackFaceCulling();
        OpenGLState.enableDepthTest();
        OpenGLState.disableAlphaBlending();
        gBuffer.bind();
        glClearColor(0.1f, 0.1f, 0.9f, 0.0f);
        gBuffer.clear();
        renderer.begin(viewMatrix,projMatrix);
        for (Island island : world.getVisibleIslands()) {
            renderer.render(island.getModel(), island.getTransformation());
        }
        //TODO: put earth management in World.java
        Matrix4f unitMatrix = new Matrix4f();
        for(Model model:world.getTerrain()) {
            renderer.render(model, unitMatrix);
        }
        renderer.render(player.getModel(),player.getTransformationMatrix());
        hizGen.generateHiZMipMap(gBuffer);
        //
        entityCollectionTimer.startQuery();
        List<Entity> visibleEntities = world.getVisibleEntities(projMatrix,viewMatrix,e->Maths.couldBeVisible(e,camPos));
        entityCollectionTimer.stopQuery();
        //TODO: more performant occlusion culling
        vegetationRenderer.prepareRender( visibleEntities);
        gBuffer.bind();
        vegetationRenderer.vegetationShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, vegetationRenderer.getWindTexture());
        vegetationRenderer.vegetationShader.loadMatrix("projMatrix", projMatrix);
        vegetationRenderer.vegetationShader.loadMatrix("viewMatrix", viewMatrix);
        vegetationRenderer.vegetationShader.loadFloat("time", time);
        vegetationRenderer.vegetationShader.loadInt("useInputTransformationMatrix", 1);
        vegetationRenderer.render(time, projMatrix, viewMatrix);
        vertexTimer.stopQuery();
        hizGen.generateHiZMipMap(gBuffer);
        shadowRenderer.render(gBuffer,projMatrix,viewMatrix,NEAR_PLANE,FAR_PLANE,FOV,aspectRatio,time,lightPos,multiIndirectRenderer);
        OpenGLState.enableAlphaBlending();
        OpenGLState.disableDepthTest();
        gBuffer.blitDepth(lightingPassRenderer.deferredResult);
        lightingPassRenderer.render(gBuffer, shadowRenderer, projMatrix, viewMatrix, lightPos, lightColor, FAR_PLANE);
        display.clear();
        postProcessPipeline.applyPostProcessing(display, lightingPassRenderer.deferredResult);
        // nuklearBinding.renderGUI(display.getWidth(),display.getHeight());
        display.flipBuffers();
    }
    //TODO: add GUI

    public void resize(int width1, int height1, float aspectRatio) {
        this.width=width1;
        this.height=height1;
        this.aspectRatio=aspectRatio;
        gBuffer.resize(width1, height1);
        lightingPassRenderer.deferredResult.resize(width1, height1);
        shadowRenderer.ssaoEffect.resize(width1, height1);
        postProcessPipeline.resize(width1, height1);
        setProjMatrix();
    }

    public BufferObject getMultiDrawVBO(){
        return multiIndirectRenderer.getPersistentMatrixVbo();
    }

    private void setProjMatrix(){
        projMatrix.setPerspective((float)Math.toRadians(FOV),aspectRatio,NEAR_PLANE,FAR_PLANE);
    }

}
