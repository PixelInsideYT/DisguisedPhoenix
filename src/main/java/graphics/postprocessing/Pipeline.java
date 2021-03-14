package graphics.postprocessing;

import graphics.camera.Camera;
import graphics.context.Display;
import graphics.objects.FrameBufferObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    private FrameBufferObject atmosphereFbo;

    private final boolean godRaysEnabled = true;
    private final GodRays rays;
    private Atmosphere atm;

    public Pipeline(int width, int height, Matrix4f projMatrix, QuadRenderer quadRenderer, GaussianBlur blur) {
        this.projMatrix = projMatrix;
        this.renderer = quadRenderer;
        bloom = new Bloom(width, height, quadRenderer);
        combine = new Combine(width, height, quadRenderer);
        //dof = new BokehDepthOfField(width,height,projMatrix,quadRenderer);
     //   dof = new BlurDepthOfField(width, height, projMatrix, renderer, blur);
        fxaa = new FXAARenderer(quadRenderer);
        //hdrResolver = new HDRToLDR(width,height,quadRenderer);
        rays = new GodRays(quadRenderer);
        atm = new Atmosphere(quadRenderer);
        atmosphereFbo =new FrameBufferObject(width,height,1).addTextureAttachment(0);
    }

    public void applyPostProcessing(Display display, Matrix4f viewMatrix, FrameBufferObject deferredResult, FrameBufferObject deferred, Vector3f lightPos, Camera ffc) {
      bloom.render(deferredResult.getTextureID(1));
      //  rays.render(projMatrix, viewMatrix, lightPos, deferred.getTextureID(1));
        combine.render(deferredResult.getTextureID(0), bloom.getTexture());
        atmosphereFbo.bind();
        atm.render(ffc,projMatrix,combine.getCombinedResult(),deferred.getDepthTexture(),lightPos);
        atmosphereFbo.unbind();
        display.setViewport();
        fxaa.render(atmosphereFbo.getTextureID(0));
        //renderer.renderTextureToScreen(atm.getLookUpTable());
    }

    public Vector3f calculateLightColor(Vector3f lightPos,Vector3f camPos){
        return atm.calculateLightColor(lightPos,camPos);
    }

    public void resize(int width, int height) {
        bloom.resize(width,height);
        combine.resize(width,height);
        atmosphereFbo.resize(width,height);
    }

    public void printTimers(){
        atm.printTimer();
    }
}
