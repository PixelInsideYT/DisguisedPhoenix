package graphics.particles;

import disuguisedPhoenix.terrain.Island;
import org.joml.SimplexNoise;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PointSeekingEmitter implements ParticleEmitter {

    private Vector3f lastPos;
    private Vector3f currentPos;
    private Vector3f endPos;
    private Vector3f movingDir;
    private float movementSpeed;
    private float ppSecond;
    private float emitterTimeCount = 0;
    private float distance;

    private Island terrain;
    private float aliveTime = 20;
    private float terrainDistance = 50f;

    public PointSeekingEmitter(Vector3f startPos, Vector3f endPos, float movementSpeed, float particlesPerSecond, Island terrain) {
        currentPos = new Vector3f(startPos);
        this.endPos = new Vector3f(endPos);
        movingDir = new Vector3f(endPos).sub(startPos).normalize();
        this.movementSpeed = movementSpeed;
        this.ppSecond = particlesPerSecond;
        distance = startPos.distance(endPos);
        this.terrain = terrain;
        lastPos = new Vector3f(currentPos);
        //TODO:make particles per second and size dependend on player distance
    }

    @Override
    public List<Particle> getParticles(float dt) {
        aliveTime -= dt;
        float blendFactor = currentPos.distance(endPos) / distance;
        Vector3f wantedDir = new Vector3f(endPos).sub(currentPos).normalize();
        float x = SimplexNoise.noise(currentPos.z / 500f, currentPos.y / 500f);
        float y = SimplexNoise.noise(currentPos.x / 500f, currentPos.z / 500f);
        float z = SimplexNoise.noise(currentPos.x / 500f, currentPos.y / 500f);
        Vector3f currentVel = new Vector3f(movingDir).add(x, y, z).normalize();
        currentVel.lerp(wantedDir, 1f - blendFactor);
        currentPos.add(currentVel.mul(movementSpeed * dt));
        currentPos.y = Math.max(currentPos.y, terrain.getHeightOfTerrain(currentPos.x, currentPos.y, currentPos.z) + terrainDistance);
        emitterTimeCount += dt;
        List<Particle> emittedParticles = new ArrayList<>();
        Random r = new Random();
        int particleCount = (int) (emitterTimeCount * ppSecond);
        for (int i = 0; i < particleCount; i++) {
            Vector3f newPos = new Vector3f(currentPos).lerp(lastPos, (float) i / (float) particleCount);
            emittedParticles.add(new Particle(newPos, new Vector3f(r.nextFloat()), r.nextFloat() * 7f, r.nextFloat() * 10f + 5f, 0.3f, new Vector4f(1, 0, 0, 1f), new Vector4f(0, 0, 1, 0.5f), 0.4f));
        }
        emitterTimeCount -= particleCount * (1f / ppSecond);
        lastPos.set(currentPos);
        return emittedParticles;
    }

    @Override
    public boolean toRemove() {
        return new Vector3f(endPos).sub(currentPos).dot(movingDir) <= 0 || endPos.distance(currentPos) <= terrainDistance * 2f || aliveTime < 0;
    }

}
