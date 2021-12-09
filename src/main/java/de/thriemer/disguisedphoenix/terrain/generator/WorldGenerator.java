package de.thriemer.disguisedphoenix.terrain.generator;

import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.engine.util.Maths;
import de.thriemer.graphics.loader.MeshInformation;
import de.thriemer.graphics.particles.ParticleManager;
import org.joml.Vector3f;
import org.spongepowered.noise.module.source.RidgedMultiSimplex;
import org.spongepowered.noise.module.source.Simplex;

public class WorldGenerator {
    RidgedMultiSimplex ridgedMultiSimplex = new RidgedMultiSimplex();
    //TODO: BiomeConfig benutzen für noise funktion
    //TODO: biomeconfig für entity placement benutzen
    TerrainGenerator terrainGenerator;

    Simplex moistureNoise = new Simplex();
    Simplex bootstrapNoise = new Simplex();
    BiomeManager biomeManager = new BiomeManager();

    private static final int SEED = 2;
    float radius;
    float max;
    float seaLevel;

    public WorldGenerator(float radius) {
        this.radius = radius;
        terrainGenerator = new TerrainGenerator();
        moistureNoise.setOctaveCount(1);
        moistureNoise.setSeed(SEED);
        bootstrapNoise.setSeed(SEED);
        bootstrapNoise.setSeed(2);
        max = scaleNoise(1f);
        seaLevel = scaleNoise(0.3f);
    }

    public MeshInformation createTerrainFor(int index) {
        return terrainGenerator.buildTerrain(6, index, this::getNoiseFunction, this::getColor);
    }

    public World generateWorld(ParticleManager pm) {
        World world = new World(pm, 4f * radius);
        return world;
    }

    public Vector3f getNoiseFunction(Vector3f v) {
        Vector3f bootstrapped = bootstrapVector(v);
        v.set(bootstrapped);
        return v;
    }

    float realHeight = 4000;

    protected static float maxHumidity = 500f;
    protected static float minTemp = -10;
    protected static float maxTemp = 40;

    public Vector3f getColor(Vector3f height, Vector3f normal) {
        float temperature = getTemperature(height);
        return biomeManager.getColor(temperature, getMoisture(height, temperature));
    }


    float tempDecreasePerMeter = 0.67f / 100f;

    private float getTemperature(Vector3f bootstrappedVector) {
        //temperature interpolated between pole and equator
        Vector3f vec = new Vector3f(bootstrappedVector).normalize();
        float d = Math.abs(vec.dot(new Vector3f(0, 1, 0)));
        float angle = (float) Math.acos(d);
        float mixAmount = angle / ((float) Math.PI / 2f);
        float longituteTemperature = Maths.lerp(maxTemp, minTemp, mixAmount);
        //temperature decrease with height
        float heightTemperature = (bootstrappedVector.length() - seaLevel) * tempDecreasePerMeter / (max - seaLevel) * realHeight;
        return longituteTemperature - heightTemperature;
    }

    private float getMoisture(Vector3f bootstrappedVector, float temperature) {
        float moistureScale = 0.001f;
        Vector3f v = new Vector3f(bootstrappedVector);
        float waterMoisture = (float) (Math.pow((v.length() - seaLevel) / (max - seaLevel), 2));
        float noiseMoisture = (float) (moistureNoise.getValue(v.x * moistureScale, v.y * moistureScale, v.z * moistureScale) / moistureNoise.getMaxValue());
        float temperatureMultiplier = (temperature - minTemp) / (maxTemp - minTemp) * 1.2f + 0.2f;
        return (noiseMoisture + waterMoisture) / 2f * maxHumidity*temperatureMultiplier;
    }


    private Vector3f bootstrapVector(Vector3f direction) {
        Vector3f v = new Vector3f(direction.normalize());
        float SIMPLEX_NOISE_SCALE = 2f;
        float val2 = (float) (bootstrapNoise.getValue(v.x * SIMPLEX_NOISE_SCALE, v.y * SIMPLEX_NOISE_SCALE, v.z * SIMPLEX_NOISE_SCALE) / bootstrapNoise.getMaxValue());
        return v.mul(scaleNoise(val2));
    }

    private float scaleNoise(float v1) {
        float v1Scale = 1f + v1 * 0.2f;
        return radius * v1Scale;
    }

    public void save() {
        biomeManager.save();
    }

}
