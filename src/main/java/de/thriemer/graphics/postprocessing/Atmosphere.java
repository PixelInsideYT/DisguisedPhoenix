package de.thriemer.graphics.postprocessing;

import de.thriemer.disguisedphoenix.Main;
import de.thriemer.disguisedphoenix.rendering.MasterRenderer;
import de.thriemer.engine.util.Maths;
import de.thriemer.graphics.camera.Camera;
import de.thriemer.graphics.core.objects.FrameBufferObject;
import de.thriemer.graphics.core.objects.GPUTimerQuery;
import de.thriemer.graphics.core.shaders.Shader;
import de.thriemer.graphics.core.shaders.ShaderFactory;
import de.thriemer.graphics.gui.Gui;
import de.thriemer.graphics.loader.TextureLoader;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL46.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Atmosphere implements Gui {

    private static final String ATMOSPHERE_RADIUS = "atmosphereRadius";
    private static final String PLANET_RADIUS_UNIFORM_NAME = "planetRadius";
    private static final String DENSITY_FALLOFF = "densityFalloff";

    private static final float[] depthCascades = new float[]{0.02f * MasterRenderer.FAR_PLANE, 0.05f * MasterRenderer.FAR_PLANE, 0.5f * MasterRenderer.FAR_PLANE, MasterRenderer.FAR_PLANE};

    private final Shader atmosphereShader;
    private final Shader lookupGenerator;
    private static final int LOOKUP_TABLE_SIZE = 512;
    private final FrameBufferObject lookUpTable;
    private final QuadRenderer renderer;
    private final Vector3f wavelengths = new Vector3f(700, 530, 440);
    private final Vector3f scatterCoeffiecients = new Vector3f();
    private float densityFalloff = 1f;
    private float scatteringStrength = 1f;
    private float atmosphereRadius = Main.radius + 12000;
    private static final float PLANET_RADIUS = Main.radius;
    private static final int NUM_OPTICAL_DEPTH_POINTS = 10;
    private final int blueNoiseTexture;
    private final GPUTimerQuery timer;
    private final FloatBuffer densityFallOffBuffer = BufferUtils.createFloatBuffer(1).put(0, densityFalloff);
    private final FloatBuffer scatterStrengthBuffer = BufferUtils.createFloatBuffer(1).put(0, scatteringStrength);
    private final FloatBuffer atmosphereRadiusBuffer = BufferUtils.createFloatBuffer(1).put(0, atmosphereRadius);

    public Atmosphere(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory atmosphereFactory = new ShaderFactory("postProcessing/atmosphere/atmosphereVS.glsl", "postProcessing/atmosphere/atmosphereFS.glsl").withAttributes("pos");
        atmosphereFactory.withUniforms("originalTexture", "depthTexture", "noiseTexture", "lookUpTexture", "shadowTexture", "cascadeDepths", "camPos", ATMOSPHERE_RADIUS, "dirToSun", PLANET_RADIUS_UNIFORM_NAME, DENSITY_FALLOFF, "scatterCoefficients", "invProjMatrix", "zNear", "zFar");
        atmosphereFactory.withUniformArray("toShadowMapCoords", 4);
        atmosphereFactory.configureSampler("originalTexture", 0);
        atmosphereFactory.configureSampler("depthTexture", 1);
        atmosphereFactory.configureSampler("noiseTexture", 2);
        atmosphereFactory.configureSampler("lookUpTexture", 3);
        atmosphereFactory.configureSampler("shadowTexture", 4);

        atmosphereFactory.configureShaderConstant("numInScatterPoints", 10);
        atmosphereFactory.configureShaderConstant("numOpticalDepthPoints", NUM_OPTICAL_DEPTH_POINTS);
        atmosphereFactory.withUniformArray("frustumRays", 4);
        atmosphereShader = atmosphereFactory.built();
        ShaderFactory atmLookupFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/atmosphere/opticalDepthPreCompute.glsl").withAttributes("pos");
        atmLookupFactory.configureShaderConstant("numOpticalDepthPoints", 200);
        lookupGenerator = atmLookupFactory.withUniforms(PLANET_RADIUS_UNIFORM_NAME, DENSITY_FALLOFF, ATMOSPHERE_RADIUS).built();
        lookUpTable = new FrameBufferObject(LOOKUP_TABLE_SIZE, LOOKUP_TABLE_SIZE, 1).addUnclampedTexture(0);
        lookUpTable.bind();
        lookupGenerator.bind();
        lookupGenerator.loadFloat(ATMOSPHERE_RADIUS, atmosphereRadius);
        lookupGenerator.loadFloat(PLANET_RADIUS_UNIFORM_NAME, PLANET_RADIUS);
        lookupGenerator.loadFloat(DENSITY_FALLOFF, densityFalloff);
        renderer.renderOnlyQuad();
        lookupGenerator.unbind();
        lookUpTable.unbind();
        //get texture values
        float[] floatValues = new float[LOOKUP_TABLE_SIZE * LOOKUP_TABLE_SIZE];
        glBindTexture(GL_TEXTURE_2D, lookUpTable.getTextureID(0));
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RED, GL_FLOAT, floatValues);
        blueNoiseTexture = TextureLoader.loadTexture("misc/blueNoise.png", GL_REPEAT, GL_NEAREST);
        timer = new GPUTimerQuery("Atmosphere");
    }

    public Vector3f calculateLightColor(Vector3f lightPos, Vector3f cameraPosition) {
        calculateScatterCoefficients();
        Vector3f toSun = new Vector3f(lightPos).sub(cameraPosition).normalize();
        Vector2f sphereTest = Maths.raySphere(new Vector3f(), atmosphereRadius, cameraPosition, toSun);
        float sunRayDepth = calculateOpticalDepth(cameraPosition, toSun, sphereTest.y);
        float transittanceR = (float) Math.exp(-(sunRayDepth) * scatterCoeffiecients.x);
        float transittanceG = (float) Math.exp(-(sunRayDepth) * scatterCoeffiecients.y);
        float transittanceB = (float) Math.exp(-(sunRayDepth) * scatterCoeffiecients.z);
        return new Vector3f(transittanceR, transittanceG, transittanceB);
    }

    private float calculateOpticalDepth(Vector3f rayOrigin, Vector3f rayDir, float rayLength) {
        Vector3f densitySamplePoint = new Vector3f(rayOrigin);
        float stepSize = rayLength / (NUM_OPTICAL_DEPTH_POINTS - 1);
        float opticalDepth = 0;
        Vector3f scaledRayDir = new Vector3f(rayDir).mul(rayDir);
        for (int i = 0; i < NUM_OPTICAL_DEPTH_POINTS; i++) {
            float heightAboveSurface = densitySamplePoint.length() - PLANET_RADIUS;
            float height01 = Math.max(heightAboveSurface / (atmosphereRadius - PLANET_RADIUS), 0f);
            float localDensity = (float) Math.exp(-height01 * densityFalloff) * (1f - height01);
            opticalDepth += localDensity * stepSize;
            densitySamplePoint.add(scaledRayDir);
        }
        return opticalDepth;
    }

    public void render(Camera camera, Matrix4f projMatrix, Matrix4f[] toShadowMap, int texture, int depthTexture, int shadowMap, Vector3f lightPos) {
        timer.startQuery();
        atmosphereShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, blueNoiseTexture);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, lookUpTable.getTextureID(0));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowMap);
        atmosphereShader.load3DVector("dirToSun", new Vector3f(lightPos).normalize());
        atmosphereShader.loadFloat("zNear", MasterRenderer.NEAR_PLANE);
        atmosphereShader.loadFloat("zFar", MasterRenderer.FAR_PLANE);
        scatteringStrength = scatterStrengthBuffer.get(0);
        atmosphereRadius = atmosphereRadiusBuffer.get(0);
        densityFalloff = densityFallOffBuffer.get(0);
        calculateScatterCoefficients();
        atmosphereShader.loadMatrix4fArray("toShadowMapCoords", toShadowMap);
        atmosphereShader.loadFloatArray("cascadeDepths", depthCascades);
        atmosphereShader.load3DVector("scatterCoefficients", scatterCoeffiecients);
        atmosphereShader.loadFloat(ATMOSPHERE_RADIUS, atmosphereRadius);
        atmosphereShader.loadFloat(PLANET_RADIUS_UNIFORM_NAME, PLANET_RADIUS);
        atmosphereShader.loadFloat(DENSITY_FALLOFF, densityFalloff);
        atmosphereShader.load3DVector("camPos", camera.getPosition());
        Matrix4f projViewMatrix = new Matrix4f(projMatrix).mul(camera.getViewMatrix());
        Matrix4f projInv = new Matrix4f(projMatrix).invert();
        atmosphereShader.loadMatrix("invProjMatrix", projInv);
        atmosphereShader.load3DVector("frustumRays[0]", projViewMatrix.frustumRayDir(0, 0, new Vector3f()));
        atmosphereShader.load3DVector("frustumRays[1]", projViewMatrix.frustumRayDir(1, 0, new Vector3f()));
        atmosphereShader.load3DVector("frustumRays[2]", projViewMatrix.frustumRayDir(0, 1, new Vector3f()));
        atmosphereShader.load3DVector("frustumRays[3]", projViewMatrix.frustumRayDir(1, 1, new Vector3f()));
        renderer.renderOnlyQuad();
        atmosphereShader.unbind();
        timer.stopQuery();
    }

    private void calculateScatterCoefficients() {
        float scatterR = (float) Math.pow(50f / wavelengths.x, 4) * scatteringStrength;
        float scatterG = (float) Math.pow(50f / wavelengths.y, 4) * scatteringStrength;
        float scatterB = (float) Math.pow(50f / wavelengths.z, 4) * scatteringStrength;
        scatterCoeffiecients.set(scatterR, scatterG, scatterB);
    }

    public int getLookUpTable() {
        return lookUpTable.getTextureID(0);
    }

    public void show(NkContext ctx, float windowWidth, float windowHeight) {
        if (false) {
            int x = 200;
            int y = 200;
            try (MemoryStack stack = stackPush()) {
                NkRect rect = NkRect.mallocStack(stack);
                if (nk_begin(
                        ctx, "Amtosphere Settings", nk_rect(x, y, 400, 250, rect), NK_WINDOW_TITLE)) {
                    nk_layout_row_dynamic(ctx, 25, 1);
                    nk_property_float(ctx, "DensityFalloff:", -10, densityFallOffBuffer, 10, 0.1f, 0.01f);
                    nk_property_float(ctx, "ScatterStrength:", 0, scatterStrengthBuffer, 5f, 0.1f, 0.01f);
                    nk_property_float(ctx, "AtmosphereRadius:", 1000, atmosphereRadiusBuffer, 1000000, 100, 100);
                }
                nk_end(ctx);
            }
        }
    }


}
