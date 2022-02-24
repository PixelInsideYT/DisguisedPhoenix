package de.thriemer.disguisedphoenix.terrain.generator;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.terrain.PositionProvider;
import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.disguisedphoenix.terrain.generator.biomes.BiomeManager;
import de.thriemer.engine.util.Maths;
import de.thriemer.engine.util.ModelFileHandler;
import de.thriemer.graphics.loader.MeshInformation;
import de.thriemer.graphics.modelinfo.Model;
import de.thriemer.graphics.particles.ParticleManager;
import org.joml.Quaternionf;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.spongepowered.noise.module.source.Billow;
import org.spongepowered.noise.module.source.RidgedMultiSimplex;
import org.spongepowered.noise.module.source.Simplex;

import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class WorldGenerator {
    //TODO: BiomeConfig benutzen für noise funktion
    //TODO: biomeconfig für entity placement benutzen
    TerrainGenerator terrainGenerator;

    Random random;

    Simplex moistureNoise = new Simplex();
    Billow bootstrapNoise = new Billow();
    RidgedMultiSimplex ridgedMultiSimplex = new RidgedMultiSimplex();
    BiomeManager biomeManager = new BiomeManager();

    public static final int SEED = 2;
    float radius;
    float max;
    float seaLevel;

    public WorldGenerator(float radius) {
        this.radius = radius;
        terrainGenerator = new TerrainGenerator();
        moistureNoise.setOctaveCount(1);
        moistureNoise.setSeed(SEED);
        bootstrapNoise.setSeed(SEED);
        max = scaleNoise(1f);
        seaLevel = scaleNoise(0.3f);
        random = new Random(SEED);
    }

    public MeshInformation createTerrainFor(Vector2i terrainIndex) {
        return terrainGenerator.buildTerrain(terrainIndex, this::getBaseHeight, this::getNoiseFunction, this::getColor);
    }

    public World generateWorld(ParticleManager pm) {
        World world = new World(pm, 4f * radius);
        return world;
    }

    public float getBaseHeight(Vector2i chunkIndex) {
        float size = TerrainGenerator.CHUNK_SIZE;
        return getNoiseFloor((chunkIndex.x + 0.5f) * size, (chunkIndex.y + 0.5f) * size);
    }

    public float getNoiseFloor(float x, float z) {
        return (float) ridgedMultiSimplex.get(x*0.0001f, 0, z*0.0001f)*1000;
    }

    public float getNoiseFunction(Vector3f v) {
        float SIMPLEX_NOISE_SCALE = 0.002f;
        float WARP_SCALE = 0.001f;

        float floor = getNoiseFloor(v.x, v.z);
        float warpX =4f*(float) moistureNoise.get(WARP_SCALE*v.x,WARP_SCALE*v.y,WARP_SCALE*v.z);
        float warpY =4f*(float) moistureNoise.get(WARP_SCALE*v.x+5.4f,WARP_SCALE*v.y-1.3f,WARP_SCALE*v.z+9.7f);
        float warpZ =4f*(float) moistureNoise.get(WARP_SCALE*v.x-3.3f,WARP_SCALE*v.y+11.5f,WARP_SCALE*v.z-23.53f);

        float noise = (float) (bootstrapNoise.get(v.x * SIMPLEX_NOISE_SCALE+warpX, v.y * SIMPLEX_NOISE_SCALE+warpY, v.z * SIMPLEX_NOISE_SCALE+warpZ)/ ridgedMultiSimplex.maxValue());
        return noise+(1-((v.y-floor)/(TerrainGenerator.DELTA_CHUNK*TerrainGenerator.CHUNK_SIZE)+1));
    }

    float realHeight = 4000;

    protected static float maxHumidity = 500f;
    public static float minTemp = -10;
    public static float maxTemp = 40;

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
        float noiseMoisture = (float) (moistureNoise.get(v.x * moistureScale, v.y * moistureScale, v.z * moistureScale) / moistureNoise.maxValue());
        float temperatureMultiplier = (temperature - minTemp) / (maxTemp - minTemp) * 1.2f + 0.2f;
        return (noiseMoisture + waterMoisture) / 2f * maxHumidity * temperatureMultiplier;
    }


    private float scaleNoise(float v1) {
        float v1Scale = 1f + v1 * 0.2f;
        return radius * v1Scale;
    }

    public void save() {
        biomeManager.save();
    }

    public void addEntities(MeshInformation meshInformation, Consumer<Entity> entityInserter) {
        PositionProvider positionProvider = new PositionProvider(meshInformation);
        float area = positionProvider.getArea();
        area=0;
        while (area > 0) {
            Map.Entry<Vector3f, Vector3f> positionAndNormal = positionProvider.getRandomPosition();
            Vector3f position = positionAndNormal.getKey();
            Vector3f normal = positionAndNormal.getValue();
            float temperature = getTemperature(position);
            String[] possibleModels = biomeManager.getModels(temperature, getMoisture(position, temperature));
            String chosen = possibleModels[random.nextInt(possibleModels.length)];
            Model model = ModelFileHandler.getModel(chosen);
            float scaleDifference = (random.nextFloat() * 2f - 1) * 0.5f + 1.0f;
            float modelAreaEstimate = (float) Math.PI * model.getRadiusXZ() * model.getRadiusXZ() * 2f * scaleDifference;
            area -= modelAreaEstimate;
            entityInserter.accept(rotateUpRight(new Entity(model, position, 0, random.nextFloat() * 7f, 0f, scaleDifference), normal));
        }
    }

    private Entity rotateUpRight(Entity e, Vector3f normal) {
        Vector3f eulerAngles = new Vector3f();
        Quaternionf qf = new Quaternionf();
        qf.rotateTo(new Vector3f(0, 1, 0), normal);
        qf.mul(new Quaternionf().rotateLocalY(e.getRotY()), qf);
        qf.getEulerAnglesXYZ(eulerAngles);
        e.setRotX(eulerAngles.x);
        e.setRotY(eulerAngles.y);
        e.setRotZ(eulerAngles.z);
        return e;
    }


}
