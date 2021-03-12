package graphics.postProcessing;

import disuguisedPhoenix.Main;
import engine.util.Maths;
import graphics.camera.Camera;
import graphics.loader.TextureLoader;
import graphics.objects.FrameBufferObject;
import graphics.objects.TimerQuery;
import graphics.shaders.Shader;
import graphics.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.CallbackI;

import java.io.PrintWriter;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;

public class Atmosphere {

    private Shader atmosphereShader;
    private Shader lookupGenerator;
    private int lookupTableSize = 512;
    private FrameBufferObject lookUpTable;
    private QuadRenderer renderer;
    private Vector3f wavelengths = new Vector3f(700, 530, 440);
    private Vector3f scatterCoeffiecients = new Vector3f();
    private float densityFalloff = 4f;
    private float scatteringStrength = 1f;
    private float atmosphereRadius = Main.radius + 12000;
    private float planetRadius = Main.radius;
    private int numOpticalDepthPoints = 10;
    private int blueNoiseTexture;
    private TimerQuery timer;

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
        float stepSize = rayLength / (numOpticalDepthPoints - 1);
        float opticalDepth = 0;
        Vector3f scaledRayDir = new Vector3f(rayDir).mul(rayDir);
        for (int i = 0; i < numOpticalDepthPoints; i++) {
            float heightAboveSurface = densitySamplePoint.length() - planetRadius;
            float height01 = Math.max(heightAboveSurface / (atmosphereRadius - planetRadius), 0f);
            float localDensity = (float) Math.exp(-height01 * densityFalloff) * (1f - height01);
            opticalDepth += localDensity * stepSize;
            densitySamplePoint.add(scaledRayDir);
        }
        return opticalDepth;
    }

    public Atmosphere(QuadRenderer renderer) {
        this.renderer = renderer;
        ShaderFactory atmosphereFactory = new ShaderFactory("postProcessing/atmosphere/atmosphereVS.glsl", "postProcessing/atmosphere/atmosphereFS.glsl").withAttributes("pos");
        atmosphereFactory.withUniforms("originalTexture", "depthTexture", "noiseTexture", "lookUpTexture", "camPos", "atmosphereRadius", "dirToSun", "planetRadius", "densityFalloff", "scatterCoefficients", "invProjMatrix", "zNear", "zFar");
        atmosphereFactory.configureSampler("originalTexture", 0);
        atmosphereFactory.configureSampler("depthTexture", 1);
        atmosphereFactory.configureSampler("noiseTexture", 2);
        atmosphereFactory.configureSampler("lookUpTexture", 3);

        atmosphereFactory.configureShaderConstant("numInScatterPoints", 10);
        atmosphereFactory.configureShaderConstant("numOpticalDepthPoints", numOpticalDepthPoints);
        atmosphereFactory.withUniformArray("frustumRays", 4);
        atmosphereShader = atmosphereFactory.built();
        ShaderFactory atmLookupFactory = new ShaderFactory("postProcessing/quadVS.glsl", "postProcessing/atmosphere/opticalDepthPreCompute.glsl").withAttributes("pos");
        atmLookupFactory.configureShaderConstant("numOpticalDepthPoints", 200);
        lookupGenerator = atmLookupFactory.withUniforms("planetRadius", "densityFalloff", "atmosphereRadius").built();
        lookUpTable = new FrameBufferObject(lookupTableSize, lookupTableSize, 1).addUnclampedTexture(0);
        lookUpTable.bind();
        lookupGenerator.bind();
        lookupGenerator.loadFloat("atmosphereRadius", atmosphereRadius);
        lookupGenerator.loadFloat("planetRadius", planetRadius);
        lookupGenerator.loadFloat("densityFalloff", densityFalloff);
        renderer.renderOnlyQuad();
        lookupGenerator.unbind();
        lookUpTable.unbind();
        //get texture values
        float[] floatValues = new float[lookupTableSize * lookupTableSize];
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, lookUpTable.getTextureID(0));
        GL13.glGetTexImage(GL13.GL_TEXTURE_2D, 0, GL_RED, GL13.GL_FLOAT, floatValues);
        try {
            PrintWriter out = new PrintWriter("atmosphereData.csv");
            out.println("angle,height,opticalDepth");
            for (int a = 0; a < lookupTableSize; a++) {
                for (int h = 0; h < lookupTableSize; h++) {
                    out.println(a/(float)lookupTableSize+","+h/(float)lookupTableSize+","+floatValues[h*lookupTableSize+a]);
                }
            }
            out.close();
        } catch (Exception x) {
            x.printStackTrace();
        }
        blueNoiseTexture = TextureLoader.loadTexture("misc/blueNoise.png", GL_REPEAT, GL_NEAREST);
        timer = new TimerQuery("Atmosphere");
    }

    public void render(Camera camera, Matrix4f projMatrix, int texture, int depthTexture, Vector3f lightPos) {
        timer.startQuery();
        atmosphereShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, texture);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, depthTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, blueNoiseTexture);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, lookUpTable.getTextureID(0));
        atmosphereShader.load3DVector("dirToSun", new Vector3f(lightPos).normalize());
        atmosphereShader.loadFloat("zNear", 1f);
        atmosphereShader.loadFloat("zFar", 100000);
        atmosphereShader.loadFloat("atmosphereRadius", atmosphereRadius);
        atmosphereShader.loadFloat("planetRadius", planetRadius);
        calculateScatterCoefficients();
        atmosphereShader.load3DVector("scatterCoefficients", scatterCoeffiecients);
        atmosphereShader.loadFloat("densityFalloff", densityFalloff);
        atmosphereShader.load3DVector("camPos", camera.position);
        Matrix4f projViewMatrix = new Matrix4f(projMatrix).mul(camera.getViewMatrix());
        Matrix4f projInv = new Matrix4f(projMatrix).invert();
        atmosphereShader.loadMatrix("invProjMatrix", projInv);
        atmosphereShader.load3DVector("frustumRays[0]", projViewMatrix.frustumRayDir(0, 0, new Vector3f()));
        atmosphereShader.load3DVector("frustumRays[1]", projViewMatrix.frustumRayDir(1, 0, new Vector3f()));
        atmosphereShader.load3DVector("frustumRays[2]", projViewMatrix.frustumRayDir(0, 1, new Vector3f()));
        atmosphereShader.load3DVector("frustumRays[3]", projViewMatrix.frustumRayDir(1, 1, new Vector3f()));
        renderer.renderOnlyQuad();
        atmosphereShader.unbind();
        timer.waitOnQuery();
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

    public void printTimer() {
        timer.printResults();
    }

}
