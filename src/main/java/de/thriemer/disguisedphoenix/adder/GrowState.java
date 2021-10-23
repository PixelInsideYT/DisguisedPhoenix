package de.thriemer.disguisedphoenix.adder;

import de.thriemer.disguisedphoenix.Entity;
import de.thriemer.disguisedphoenix.terrain.World;
import de.thriemer.graphics.particles.ParticleManager;
import de.thriemer.graphics.particles.PointSeekingEmitter;
import de.thriemer.graphics.particles.UpwardsParticles;
import org.joml.Vector3f;

public class GrowState {

    Entity growingEntity;
    float buildProgress;
    PointSeekingEmitter entitySeeker;
    UpwardsParticles growParticles;
    private boolean seekerReachedEntity = false;
    private boolean addedToIsland = false;
    private final World world;

    public GrowState(World world, int particlesCount, Vector3f playerPos, Entity toGrow, ParticleManager pm) {
        entitySeeker = new PointSeekingEmitter(playerPos, toGrow.getPosition(), 15, 700f, particlesCount, world);
        pm.addParticleEmitter(entitySeeker);
        buildProgress = -0.01f;
        this.world=world;
        this.growingEntity = toGrow;
    }

    public void update(float dt, float builtSpeed, float particlesPerSecondPerAreaUnit, ParticleManager pm) {
        if (!seekerReachedEntity && entitySeeker.toRemove()) {
            seekerReachedEntity = true;
            //init upward particle spawner
            float emitTime = 1f / builtSpeed;
            float radius = growingEntity.getScale() * growingEntity.getModel().getRadiusXZ();
            growParticles = new UpwardsParticles(new Vector3f(growingEntity.getPosition()), radius, 500, 3.14f * radius * radius * particlesPerSecondPerAreaUnit, emitTime);
            pm.addParticleEmitter(growParticles);
        }
        if (seekerReachedEntity) {
         //   growParticles.center.y += dt * builtSpeed * growingEntity.getModel().getHeight() * growingEntity.getScale();
            buildProgress += dt * builtSpeed;
        }
    }

    public boolean isFullyGrown() {
        return buildProgress >= 1;
    }

    public void addToIsland() {
        if (!addedToIsland) {
            addedToIsland = true;
            world.addEntity(growingEntity);
        }
    }

    public boolean isReachedBySeeker() {
        return seekerReachedEntity;
    }

}
