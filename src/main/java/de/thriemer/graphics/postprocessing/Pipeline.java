package de.thriemer.graphics.postprocessing;

import de.thriemer.graphics.camera.Camera;
import de.thriemer.graphics.core.context.Display;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Pipeline {

    private final Bloom bloom;

    private final Combine combine;

    private final boolean fxaaEnabled = true;
    private final FXAARenderer fxaa;

    private final Atmosphere atm;

    public Pipeline(int width, int height, QuadRenderer quadRenderer, GaussianBlur blur) {
        bloom = new Bloom(width, height, quadRenderer);
        combine = new Combine(width, height, quadRenderer);
        fxaa = new FXAARenderer(quadRenderer);
        atm = new Atmosphere(quadRenderer);
    }

    public void applyPostProcessing(Display display,Matrix4f projMatrix, FrameBufferObject deferredResult,int depthTexture, Camera camera, Matrix4f[] toShadowMap, int shadowMap, Vector3f lightPos){
      //  bloom.render(deferredResult.getTextureID(1));
      //  combine.render(deferredResult.getTextureID(0), bloom.getTexture());
        display.setViewport();
      //  atm.render(camera, projMatrix, toShadowMap, combine.getCombinedResult(), depthTexture ,shadowMap, lightPos);
        fxaa.render(deferredResult.getTextureID(0));
    }

    public void resize(int width, int height) {
        bloom.resize(width, height);
        combine.resize(width, height);
    }

}
