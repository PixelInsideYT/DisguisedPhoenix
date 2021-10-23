package de.thriemer.graphics.particles;

import java.util.List;

public interface ParticleEmitter {

    List<Particle> getParticles(float dt);

    boolean toRemove();
}
