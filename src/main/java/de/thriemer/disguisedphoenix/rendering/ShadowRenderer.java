package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.graphics.core.context.ContextInformation;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.renderer.MultiIndirectRenderer;
import de.thriemer.graphics.occlusion.CSMResolver;
import de.thriemer.graphics.occlusion.SSAOEffect;
import de.thriemer.graphics.occlusion.ShadowEffect;
import de.thriemer.graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShadowRenderer {

    protected ShadowEffect shadowEffect;
    protected SSAOEffect ssaoEffect;
    CSMResolver csmResolver;

    public ShadowRenderer(QuadRenderer quadRenderer, ContextInformation contextInformation) {
        ssaoEffect = new SSAOEffect(quadRenderer, contextInformation.getWidth(), contextInformation.getHeight());
        ssaoEffect.disable();
        shadowEffect = new ShadowEffect(contextInformation);
      //  shadowEffect.disable();
        csmResolver = new CSMResolver(quadRenderer,contextInformation);

    }

    public void render(FrameBufferObject gBuffer, CameraInformation cameraInformation, float time, Vector3f lightPos, World world, MultiIndirectRenderer multiRenderer) {
        shadowEffect.render(cameraInformation, time, lightPos, world, multiRenderer);
        if(shadowEffect.isEnabled())
            csmResolver.render(cameraInformation,gBuffer.getDepthTexture(), shadowEffect);
        ssaoEffect.renderEffect(gBuffer, cameraInformation);
    }

    public int getShadowTexture() {
        return csmResolver.getShadowTexture();
    }

    public void resize(ContextInformation contextInformation){
        csmResolver.resize(contextInformation);
        ssaoEffect.resize(contextInformation.getWidth(),contextInformation.getHeight());
    }
}
