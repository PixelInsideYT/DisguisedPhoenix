package disuguisedphoenix.terrain.generator;

import disuguisedphoenix.terrain.World;
import graphics.loader.MeshInformation;
import graphics.particles.ParticleManager;
import org.joml.Vector3f;
import org.spongepowered.noise.module.source.RidgedMultiSimplex;
import org.spongepowered.noise.module.source.Simplex;

//TODO: lazy octree construction
public class WorldGenerator {
    RidgedMultiSimplex ridgedMultiSimplex = new RidgedMultiSimplex();
    Simplex simplex = new Simplex();
    //TODO: BiomeConfig benutzen für noise funktion
    //TODO: biomeconfig für entity placement benutzen
    TerrainGenerator terrainGenerator;
    private static final float BIOME_SELECTION_NOISE_SCALE = 0.5f;
    private static final float RIDGE_NOISE_SCALE = 1f;
    private static final float SIMPLEX_NOISE_SCALE = 0.7f;


    private static final float RIDGE_CHANGE = 0.3f;
    private static final float SIMPLEX_CHANGE = 0.15f;

    float radius;
    BiomeConfiguration configuration;

    public WorldGenerator(float radius) {
        this.radius = radius;
        configuration = new BiomeConfiguration(radius);
        terrainGenerator = new TerrainGenerator();
    }

    public MeshInformation createTerrainFor(Vector3f vector3f) {
        return terrainGenerator.buildTerrain(6, vector3f, this::getNoiseFunction, this::getColor);
    }


    public World generateWorld(ParticleManager pm) {
        World world = new World(pm, 4f * radius);
        return world;
    }

    float max = -Float.MAX_VALUE;
    float min = Float.MAX_VALUE;

    public Vector3f getNoiseFunction(Vector3f v) {
        v.normalize();
        float biomeSelection = (float) simplex.getValue(v.x * BIOME_SELECTION_NOISE_SCALE, v.y * BIOME_SELECTION_NOISE_SCALE, v.z * BIOME_SELECTION_NOISE_SCALE) / 2f + 0.5f;
        float val1 = (float) ridgedMultiSimplex.getValue(v.x * RIDGE_NOISE_SCALE, v.y * RIDGE_NOISE_SCALE, v.z * RIDGE_NOISE_SCALE);
        float val2 = (float) simplex.getValue(v.x * SIMPLEX_NOISE_SCALE + 4, v.y * SIMPLEX_NOISE_SCALE + 6, v.z * SIMPLEX_NOISE_SCALE + 7);
        float noise = biomeSelection * val1 * RIDGE_CHANGE + (1f - biomeSelection) * val2 * SIMPLEX_CHANGE;
        v.mul(radius * (1f + noise));
        max = Math.max(v.length(), max);
        min = Math.min(v.length(), min);
        return v;
    }

    public Vector3f getColor(Vector3f avgHeight, Vector3f normal) {
        Vector3f position = new Vector3f(avgHeight);
        float dot = Math.abs(normal.dot(avgHeight.normalize()));
        Vector3f green = new Vector3f(34f / 255f, 139f / 255f, 34f / 255f);
        Vector3f rock = new Vector3f(0.9412f, 0.8941f, 0.8235f);
        return rock.lerp(green, dot);
    }


}
