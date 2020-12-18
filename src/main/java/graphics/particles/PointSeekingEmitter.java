package graphics.particles;

import disuguisedPhoenix.terrain.Island;
import disuguisedPhoenix.terrain.World;
import org.joml.SimplexNoise;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PointSeekingEmitter implements ParticleEmitter {

    private final Vector3f lastPos;
    private final Vector3f currentPos;
    private final Vector3f endPos;
    private final Vector3f movingDir;
    private final float movementSpeed;
    private final float startSize;
    private final float variation = 0.2f;
    private final float ppSecond;
    private float emitterTimeCount = 0;
    private final float distance;

    private final World heightLookup;
    private float aliveTime;
    private final float terrainDistance = 50f;

    private Vector3f islandDodgePos = null;
    private Vector3f islandDogeMovementDir = null;


    public PointSeekingEmitter(Vector3f startPos, Vector3f endPos, float startSize, float movementSpeed, float particlesPerSecond, World world) {
        currentPos = new Vector3f(startPos);
        this.endPos = new Vector3f(endPos);
        movingDir = new Vector3f(endPos).sub(startPos).normalize();
        this.movementSpeed = movementSpeed;
        this.ppSecond = particlesPerSecond;
        distance = startPos.distance(endPos);
        this.heightLookup = world;
        lastPos = new Vector3f(currentPos);
        this.startSize = startSize;
        aliveTime = 1.5f * distance / movementSpeed;
    }

    float angleToDodge = 0f;

    @Override
    public List<Particle> getParticles(float dt) {
        aliveTime -= dt;
        float blendFactor = currentPos.distance(endPos) / distance;
        Vector3f wantedDir = new Vector3f(endPos).sub(currentPos).normalize();
        float x = SimplexNoise.noise(currentPos.z / 500f, currentPos.y / 500f);
        float y = SimplexNoise.noise(currentPos.x / 500f, currentPos.z / 500f);
        float z = SimplexNoise.noise(currentPos.x / 500f, currentPos.y / 500f);
        Vector3f currentVel = new Vector3f(islandDogeMovementDir == null ? movingDir : islandDogeMovementDir).add(x, y, z).normalize();
        currentVel.lerp(wantedDir, 1f - blendFactor);
        List<Island> possibleHeightCollisions = heightLookup.getPossibleTerrainCollisions(currentPos);
        currentPos.add(currentVel.mul(movementSpeed * dt));
        for (Island land : possibleHeightCollisions) {
            //TODO: point seeking emmiter avoids islands and doesnt get stuck
            float terrainTop = land.getHeightOfTerrain(currentPos.x, currentPos.y, currentPos.z);
            float terrainBot = land.getBottemOfTerrain(currentPos.x, currentPos.y, currentPos.z);
            if(terrainTop!=- Float.MAX_VALUE||terrainBot!=Float.MAX_VALUE){
            if (Math.abs(currentPos.y - terrainTop) < Math.abs(currentPos.y - terrainBot)) {
                if (currentPos.y < terrainTop + terrainDistance) {
                    currentPos.y = terrainTop + terrainDistance;
                    if (islandDodgePos==null&&new Vector3f(0, -1, 0).dot(new Vector3f(currentVel).normalize()) > angleToDodge) {
                        islandDodgePos = new Vector3f(land.position).sub(0, 2 * terrainDistance, 0);
                        islandDogeMovementDir = new Vector3f(islandDodgePos).sub(currentPos).normalize();
                        aliveTime += 2f * islandDodgePos.distance(currentPos) / movementSpeed;
                    }
                }
            } else {
                currentPos.y = Math.min(currentPos.y, terrainBot - terrainDistance);
                if (currentPos.y > terrainBot - terrainDistance) {
                    currentPos.y = terrainBot - terrainDistance;
                    if (islandDodgePos==null&&new Vector3f(0, 1, 0).dot(new Vector3f(currentVel).normalize()) > angleToDodge) {
                        islandDodgePos = new Vector3f(land.position).add(0, 2 * terrainDistance, 0);
                        islandDogeMovementDir = new Vector3f(islandDodgePos).sub(currentPos).normalize();
                        aliveTime += 2f * islandDodgePos.distance(currentPos) / movementSpeed;
                    }
                }}
            }
        }
        if (islandDodgePos != null && currentPos.distance(islandDodgePos) < 2 * movementSpeed * dt) {
            islandDodgePos = null;
            islandDogeMovementDir = null;
        }
        emitterTimeCount += dt;
        List<Particle> emittedParticles = new ArrayList<>();
        Random r = new Random();
        int particleCount = (int) (emitterTimeCount * ppSecond);
        for (int i = 0; i < particleCount; i++) {
            Vector3f newPos = new Vector3f(currentPos).lerp(lastPos, (float) i / (float) particleCount);
            emittedParticles.add(new Particle(newPos, new Vector3f(r.nextFloat()), (r.nextFloat() * 2f - 1f) * 7f, (1f + r.nextFloat() * variation - 2 * variation) * startSize, 0.3f, new Vector4f(1, 0, 0, 1f), new Vector4f(0, 0, 1, 0.5f), 0.4f));
        }
        emitterTimeCount -= particleCount * (1f / ppSecond);
        lastPos.set(currentPos);
        return emittedParticles;
    }

    @Override
    public boolean toRemove() {
        return new Vector3f(endPos).sub(currentPos).dot(movingDir) <= 0 || endPos.distance(currentPos) <= terrainDistance*1.5f || aliveTime < 0;
    }

}
