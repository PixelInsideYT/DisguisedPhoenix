package disuguisedphoenix.adder;

import disuguisedphoenix.Entity;
import disuguisedphoenix.terrain.PopulatedIsland;
import disuguisedphoenix.terrain.World;
import graphics.particles.ParticleManager;
import graphics.particles.PointSeekingEmitter;
import graphics.particles.UpwardsParticles;
import org.joml.Vector3f;

public class GrowState {

    Entity growingEntity;
    float buildProgress;
    PointSeekingEmitter entitySeeker;
    UpwardsParticles growParticles;
    private boolean seekerReachedEntity = false;

    public PopulatedIsland growingOn;
    private boolean addedToIsland = false;

    public GrowState(World world, PopulatedIsland growingOn, int particlesCount, Vector3f playerPos, Entity toGrow, ParticleManager pm) {
        entitySeeker = new PointSeekingEmitter(playerPos, toGrow.getPosition(), 15, 700f, particlesCount, world);
        pm.addParticleEmitter(entitySeeker);
        buildProgress = -0.01f;
        this.growingOn = growingOn;
        this.growingEntity = toGrow;
    }

    public void update(float dt, float builtSpeed, float particlesPerSecondPerAreaUnit, ParticleManager pm) {
        if (!seekerReachedEntity && entitySeeker.toRemove()) {
            seekerReachedEntity = true;
            //init upward particle spawner
            float emitTime = 1f / builtSpeed;
            float radius = growingEntity.getScale() * growingEntity.getModel().radiusXZ;
            growParticles = new UpwardsParticles(new Vector3f(growingEntity.getPosition()), radius, 500, 3.14f * radius * radius * particlesPerSecondPerAreaUnit, emitTime);
            pm.addParticleEmitter(growParticles);
        }
        if (seekerReachedEntity) {
            growParticles.center.y += dt * builtSpeed * growingEntity.getModel().height * growingEntity.getScale();
            buildProgress += dt * builtSpeed;
        }
    }

    public boolean isFullyGrown() {
        return buildProgress >= 1;
    }

    public void addToIsland() {
        if (!addedToIsland) {
            addedToIsland = true;
            growingOn.addEntity(growingEntity);
        }
    }

    public boolean isReachedBySeeker() {
        return seekerReachedEntity;
    }

}
