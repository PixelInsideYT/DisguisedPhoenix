package de.thriemer.graphics.modelinfo;

import de.thriemer.engine.collision.Collider;
import de.thriemer.graphics.loader.MeshInformation;
import lombok.Getter;
import org.joml.Vector3f;

@Getter
public class Model {

    private RenderInfo renderInfo;
    private Collider collider;

    private Vector3f relativeCenter;
    private Vector3f minAABB;
    private Vector3f maxAABB;
    private float radiusXZ;
    private float radius;

    public Model(RenderInfo info, Vector3f relativeCenter, float radiusXZ, float radius, Vector3f minAABB, Vector3f maxAABB) {
        this.renderInfo = info;
        this.radius = radius;
        this.radiusXZ = radiusXZ;
        this.relativeCenter = relativeCenter;
        this.minAABB = minAABB;
        this.maxAABB = maxAABB;
    }

    public Model(RenderInfo info, Vector3f relativeCenter, float radiusXZ, float radius, Vector3f minAABB, Vector3f maxAABB, Collider collider) {
        this(info, relativeCenter, radiusXZ, radius, minAABB, maxAABB);
        this.collider = collider;
    }

    public Model(RenderInfo info, MeshInformation meshInformation) {
        this(info, meshInformation.centerPoint, meshInformation.radiusXZPlane, meshInformation.radius, meshInformation.minAABB, meshInformation.maxAABB, meshInformation.collider);
    }

}
