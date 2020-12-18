package graphics.postProcessing;

import graphics.context.Display;
import graphics.objects.BufferObject;
import graphics.objects.FrameBufferObject;
import graphics.objects.TimerQuery;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL45;

public class Pipeline {

    private final QuadRenderer renderer;
    //private final TimerQuery hdrTimer = new TimerQuery("HDR Resolve: ");
    //private final HDRToLDR hdrResolver;
    private final Matrix4f projMatrix;

    private final boolean bloomEnabled = true;
    private final Bloom bloom;

    private final Combine combine;

    //private final boolean depthOfFieldEnabled = true;
    //private final TimerQuery dofTimer = new TimerQuery("DOF: ");
    //private BokehDepthOfField dof;
    //private final BlurDepthOfField dof;

    private final boolean fxaaEnabled = true;
    private final FXAARenderer fxaa;

    private final boolean godRaysEnabled = true;
    private final GodRays rays;

    public Pipeline(int width, int height, Matrix4f projMatrix, QuadRenderer quadRenderer, GaussianBlur blur) {
        this.projMatrix = projMatrix;
        this.renderer = quadRenderer;
        bloom = new Bloom(width, height, quadRenderer);
        combine = new Combine(width, height, quadRenderer);
        //dof = new BokehDepthOfField(width,height,projMatrix,quadRenderer);
        //dof = new BlurDepthOfField(width, height, projMatrix, renderer, blur);
        fxaa = new FXAARenderer(width, height, quadRenderer);
        //hdrResolver = new HDRToLDR(width,height,quadRenderer);
        rays = new GodRays(quadRenderer);
    }

    public void applyPostProcessing(Display display, Matrix4f viewMatrix, FrameBufferObject deferredResult, FrameBufferObject deferred, Vector3f lightPos) {
      /*  dof.computeCameraParams(deferred.getDepthTexture());
        bloom.render(deferredResult.getTextureID(1));
        rays.render(projMatrix, viewMatrix, lightPos, deferred.getTextureID(1));
        combine.render(deferredResult.getTextureID(0), bloom.getTexture(), rays.getTexture());
        dofTimer.startQuery();
        dof.render(combine.getCombinedResult(), deferred.getDepthTexture());
        dofTimer.waitOnQuery();*/
        display.setViewport();
        fxaa.render(deferredResult.getTextureID(0));
    }
}
