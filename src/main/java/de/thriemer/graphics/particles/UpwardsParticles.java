package de.thriemer.graphics.particles;

import de.thriemer.engine.util.Maths;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UpwardsParticles implements ParticleEmitter {

    private final float ppSecond;
    private final float lifeLength = 4f;
    private final float particleSpeed;
    private final float spawnRadius;
    public Vector3f center;
    Random rnd = new Random();
    private float emitterTimeCount;
    private float emitTime;


    public UpwardsParticles(Vector3f center, float spawnRadius, float travelDistance, float pps, float emitTime) {
        this.center = center;
        this.ppSecond = pps;
        particleSpeed = travelDistance / lifeLength;
        this.emitTime = emitTime;
        this.spawnRadius = spawnRadius;
    }

    @Override
    public List<Particle> getParticles(float dt) {

        emitterTimeCount += dt;
        List<Particle> emittedParticles = new ArrayList<>();
        int particleCount = (int) (emitterTimeCount * ppSecond);
        for (int i = 0; i < particleCount; i++) {
            Vector2f spawnXZPlane = Maths.createUnitVecFromAngle(rnd.nextFloat() * 7f).mul(rnd.nextFloat() * spawnRadius);
            Vector3f spawnpoint = new Vector3f(center).add(spawnXZPlane.x, 0, spawnXZPlane.y);
            Vector3f velocity = new Vector3f((float) Math.random() * 2f - 1f, (float) Math.random() + 0.5f, (float) Math.random() * 2f - 1f);
            velocity.mul(this.particleSpeed);
            emittedParticles.add(new Particle(spawnpoint, velocity, rnd.nextFloat() * 7f, rnd.nextFloat() + 0.1f, 0.1f, new Vector4f(112f / 255f, 72f / 255f, 60f / 255f, 1), new Vector4f(56f / 255f, 188f / 255f, 28f / 255f, 0.25f), lifeLength * (float) (1 + Math.random() * 0.2f - 0.4f)));
        }
        emitterTimeCount -= particleCount * (1f / ppSecond);
        emitTime -= dt;
        return emittedParticles;
    }

    @Override
    public boolean toRemove() {
        return emitTime < 0f;
    }
}
