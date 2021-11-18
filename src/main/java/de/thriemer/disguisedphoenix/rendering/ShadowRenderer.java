package de.thriemer.disguisedphoenix.rendering;

import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.renderer.MultiIndirectRenderer;
import de.thriemer.graphics.occlusion.SSAOEffect;
import de.thriemer.graphics.occlusion.ShadowEffect;
import de.thriemer.graphics.postprocessing.QuadRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ShadowRenderer {

    protected ShadowEffect shadowEffect;
    protected SSAOEffect ssaoEffect;

    public ShadowRenderer(QuadRenderer quadRenderer, int width, int height) {
        ssaoEffect = new SSAOEffect(quadRenderer, width, height);
        ssaoEffect.disable();
        shadowEffect = new ShadowEffect();
        shadowEffect.disable();
    }

    public void render(FrameBufferObject gBuffer,Matrix4f projMatrix,Matrix4f viewMatrix, float nearPlane, float farPlane, float fov, float aspectRatio, float time, Vector3f lightPos, MultiIndirectRenderer multiRenderer){
        shadowEffect.render(viewMatrix, nearPlane, farPlane, (float) Math.toRadians(fov), aspectRatio, time, lightPos, multiRenderer);
        ssaoEffect.renderEffect(gBuffer, projMatrix,farPlane);
    }

}
