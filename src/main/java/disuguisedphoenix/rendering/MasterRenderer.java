package disuguisedphoenix.rendering;

import disuguisedphoenix.Entity;
import disuguisedphoenix.terrain.Island;
import disuguisedphoenix.terrain.World;
import engine.world.Octree;
import graphics.core.context.Display;
import graphics.core.objects.BufferObject;
import graphics.core.objects.FrameBufferObject;
import graphics.core.objects.OpenGLState;
import graphics.core.objects.TimerQuery;
import graphics.core.renderer.MultiIndirectRenderer;
import graphics.core.renderer.TestRenderer;
import graphics.modelinfo.Model;
import graphics.postprocessing.GaussianBlur;
import graphics.postprocessing.HIZGenerator;
import graphics.postprocessing.Pipeline;
import graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

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
    TimerQuery vertexTimer = new TimerQuery("Geometry Pass");

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
        projMatrix.identity().perspective((float) Math.toRadians(FOV), aspectRatio, NEAR_PLANE, FAR_PLANE);
        multiIndirectRenderer = new MultiIndirectRenderer();
        vegetationRenderer = new VegetationRenderer(multiIndirectRenderer);
        renderer = new TestRenderer(vegetationRenderer.vegetationShader);
        lightingPassRenderer = new LightingPassRenderer(quadRenderer,width,height);
        shadowRenderer = new ShadowRenderer(quadRenderer,width,height);
        setupFBOs(width,height);
        setupPostProcessing(width,height,projMatrix);
        this.width=width;
        this.height=height;
        this.aspectRatio=aspectRatio;
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

    public void render(Display display,Matrix4f viewMatrix, float time, World world, Vector3f lightPos, Vector3f lightColor, Model model) {
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
        renderer.render(model,new Matrix4f());
        hizGen.generateHiZMipMap(gBuffer);
        //
        List<Octree> visibleNodes = world.getVisibleNodes(projMatrix,viewMatrix);
        List<Entity> notOccluded = occlusionCalculator.getVisibleEntities(gBuffer.getDepthTexture(),visibleNodes,new Matrix4f(projMatrix).mul(viewMatrix),width,height);
        //
        gBuffer.bind();
        vegetationRenderer.vegetationShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, vegetationRenderer.getWindTexture());
        vegetationRenderer.vegetationShader.loadMatrix("projMatrix", projMatrix);
        vegetationRenderer.vegetationShader.loadMatrix("viewMatrix", viewMatrix);
        vegetationRenderer.vegetationShader.loadFloat("time", time);
        vegetationRenderer.vegetationShader.loadInt("useInputTransformationMatrix", 1);
        vegetationRenderer.render(time, projMatrix, viewMatrix, notOccluded);
        vertexTimer.waitOnQuery();
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
        gBuffer.resize(width1, height1);
        lightingPassRenderer.deferredResult.resize(width1, height1);
        shadowRenderer.ssaoEffect.resize(width1, height1);
        postProcessPipeline.resize(width1, height1);
        projMatrix.identity().perspective((float) Math.toRadians(70), aspectRatio, NEAR_PLANE, FAR_PLANE);
        this.width=width1;
        this.height=height1;
        this.aspectRatio=aspectRatio;
    }

    public void print(){
        vertexTimer.printResults();
        lightingPassRenderer.lightTimer.printResults();
        shadowRenderer.print();
    }

    public BufferObject getMultiDrawVBO(){
        return multiIndirectRenderer.getPersistentMatrixVbo();
    }

}
