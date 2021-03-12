package disuguisedPhoenix;

import graphics.world.Model;
import org.joml.Vector3f;

import java.util.List;

public class RotatingEntity extends Entity {
    public RotatingEntity(Model m, float dx, float y, float z, float scale) {
        super(m, new Vector3f(dx, y, z), 0, 0, 0, scale);
    }

    public RotatingEntity(Model m, float dx, float scale) {
        super(m, new Vector3f(dx, 0, 0), 0, 0, 0, scale);
    }

    public void update(float dt, List<Entity> posCollisions) {
        rotY += 0.3f * (float) Math.PI * dt;
        super.update(dt, posCollisions,null);
    }

}
