package graphics.modelinfo;

import engine.collision.Collider;
import graphics.loader.MeshInformation;
import lombok.Getter;
import org.joml.Vector3f;

@Getter
public class Model {

    private RenderInfo renderInfo;
    private Collider collider;

    private Vector3f relativeCenter;
    private float height;
    private float radiusXZ;
    private float radius;

    public Model(RenderInfo info, Vector3f relativeCenter, float height, float radiusXZ, float radius) {
        this.renderInfo = info;
        this.height = height;
        this.radius = radius;
        this.radiusXZ = radiusXZ;
        this.relativeCenter = relativeCenter;
    }

    public Model(RenderInfo info, Vector3f relativeCenter, float height, float radiusXZ, float radius, Collider collider) {
        this(info, relativeCenter, height, radiusXZ, radius);
        this.collider = collider;
    }

    public Model(RenderInfo info, MeshInformation meshInformation) {
        this(info, meshInformation.centerPoint, meshInformation.height, meshInformation.radiusXZPlane, meshInformation.radius, meshInformation.collider);
    }

}
