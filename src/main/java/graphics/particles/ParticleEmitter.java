package graphics.particles;

import java.util.List;

public interface ParticleEmitter {

    List<Particle> getParticles(float dt);

    boolean toRemove();
}
