package graphics.modelinfo;

import engine.collision.Collider;
import graphics.loader.MeshInformation;
import org.joml.Vector3f;

public class Model {

    public RenderInfo renderInfo;
    public Collider collider;

    public Vector3f relativeCenter;
    public float height;
    public float radiusXZ;
    public float radius;

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
