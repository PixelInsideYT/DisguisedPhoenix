package graphics.postprocessing;

import graphics.camera.Camera;
import graphics.context.Display;
import graphics.objects.FrameBufferObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Pipeline {

    private final Matrix4f projMatrix;

    private final Bloom bloom;

    private final Combine combine;

    private final boolean fxaaEnabled = true;
    private final FXAARenderer fxaa;
    private FrameBufferObject atmosphereFbo;

    private Atmosphere atm;

    public Pipeline(int width, int height, Matrix4f projMatrix, QuadRenderer quadRenderer, GaussianBlur blur) {
        this.projMatrix = projMatrix;
        bloom = new Bloom(width, height, quadRenderer);
        combine = new Combine(width, height, quadRenderer);
        fxaa = new FXAARenderer(quadRenderer);
        atm = new Atmosphere(quadRenderer);
        atmosphereFbo = new FrameBufferObject(width, height, 1).addTextureAttachment(0);
    }

    public void applyPostProcessing(Display display, Matrix4f[] toShadowMatrix, FrameBufferObject deferredResult, FrameBufferObject deferred,int shadowTexture, Vector3f lightPos, Camera ffc) {
        bloom.render(deferredResult.getTextureID(1));
        combine.render(deferredResult.getTextureID(0), bloom.getTexture());
        atmosphereFbo.bind();
        atm.render(ffc, projMatrix,toShadowMatrix, combine.getCombinedResult(), deferred.getDepthTexture(),shadowTexture, lightPos);
        atmosphereFbo.unbind();
        display.setViewport();
        fxaa.render(atmosphereFbo.getTextureID(0));
    }

    public Vector3f calculateLightColor(Vector3f lightPos, Vector3f camPos) {
        return atm.calculateLightColor(lightPos, camPos);
    }

    public void resize(int width, int height) {
        bloom.resize(width, height);
        combine.resize(width, height);
        atmosphereFbo.resize(width, height);
    }

    public void printTimers() {
        atm.printTimer();
    }

    public Atmosphere getAtmosphere(){
        return atm;
    }
}
