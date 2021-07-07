package disuguisedphoenix.rendering;

import disuguisedphoenix.terrain.Island;
import disuguisedphoenix.terrain.World;
import graphics.core.context.Display;
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

import static org.lwjgl.opengl.GL11.glClearColor;

public class MasterRenderer {

    public static final float FAR_PLANE = 500f;
    public static final float NEAR_PLANE = 0.05f;
    public static final float FOV = 70;

    TimerQuery vertexTimer = new TimerQuery("Geometry Pass");

    Matrix4f projMatrix = new Matrix4f();

    private FrameBufferObject gBuffer;

    VegetationRenderer vegetationRenderer;
    LightingPassRenderer lightingPassRenderer;

    public MultiIndirectRenderer multiIndirectRenderer;
    QuadRenderer quadRenderer = new QuadRenderer();
    GaussianBlur blur = new GaussianBlur(quadRenderer);
    OcclusionRenderer occlusionRenderer;
    HIZGenerator hizGen = new HIZGenerator(quadRenderer);
    Pipeline postProcessPipeline;
    private TestRenderer renderer;

    public MasterRenderer(int width, int height, float aspectRatio){
        projMatrix.identity().perspective((float) Math.toRadians(FOV), aspectRatio, NEAR_PLANE, FAR_PLANE);
        multiIndirectRenderer = new MultiIndirectRenderer();
        vegetationRenderer = new VegetationRenderer(multiIndirectRenderer);
        renderer = new TestRenderer(vegetationRenderer.vegetationShader);
        lightingPassRenderer = new LightingPassRenderer(quadRenderer,width,height);
        occlusionRenderer = new OcclusionRenderer(quadRenderer,width,height,projMatrix);
        setupFBOs(width,height);
        setupPostProcessing(width,height,projMatrix);
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
    }


    public void render(Display display,Matrix4f viewMatrix, float time, World world, Vector3f camPos, Vector3f lightPos, Vector3f lightColor, Model model) {
        vertexTimer.startQuery();
        OpenGLState.enableBackFaceCulling();
        OpenGLState.enableDepthTest();
        OpenGLState.disableAlphaBlending();
        gBuffer.bind();
        glClearColor(0.1f, 0.1f, 0.9f, 0.0f);
        gBuffer.clear();
        vegetationRenderer.render(time, projMatrix, viewMatrix, world.getVisibleEntities(projMatrix, viewMatrix, camPos));
        renderer.begin(viewMatrix,projMatrix);
        for (Island island : world.getVisibleIslands()) {
            renderer.render(island.model, island.transformation);
        }
        //TODO: put earth management in World.java
        renderer.render(model,new Matrix4f());
        gBuffer.unbind();
        vertexTimer.waitOnQuery();
        OpenGLState.enableAlphaBlending();
        display.clear();
        hizGen.generateHiZMipMap(gBuffer);
        OpenGLState.disableDepthTest();
        gBuffer.blitDepth(lightingPassRenderer.deferredResult);
        lightingPassRenderer.render(gBuffer, occlusionRenderer, projMatrix, viewMatrix, lightPos, lightColor, FAR_PLANE);
        postProcessPipeline.applyPostProcessing(display, lightingPassRenderer.deferredResult);
        // nuklearBinding.renderGUI(display.getWidth(),display.getHeight());
        display.flipBuffers();
    }

    public void resize(int width1, int height1, float aspectRatio) {
        gBuffer.resize(width1, height1);
        lightingPassRenderer.deferredResult.resize(width1, height1);
        occlusionRenderer.ssaoEffect.resize(width1, height1);
        postProcessPipeline.resize(width1, height1);
        projMatrix.identity().perspective((float) Math.toRadians(70), aspectRatio, NEAR_PLANE, FAR_PLANE);
    }

}
