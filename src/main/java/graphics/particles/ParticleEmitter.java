package graphics.particles;

import java.util.List;

public interface ParticleEmitter {

    public List<Particle> getParticles(float dt);
    public boolean toRemove();
}
