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
    private Vector3f topLeftAABB;
    private Vector3f botRightAABB;
    private float radiusXZ;
    private float radius;

    public Model(RenderInfo info, Vector3f relativeCenter, float radiusXZ, float radius, Vector3f topLeftAABB, Vector3f botRightAABB) {
        this.renderInfo = info;
        this.radius = radius;
        this.radiusXZ = radiusXZ;
        this.relativeCenter = relativeCenter;
        this.topLeftAABB = topLeftAABB;
        this.botRightAABB = botRightAABB;
    }

    public Model(RenderInfo info, Vector3f relativeCenter, float radiusXZ, float radius, Vector3f topLeftAABB, Vector3f botRightAABB, Collider collider) {
        this(info, relativeCenter, radiusXZ, radius, topLeftAABB, botRightAABB);
        this.collider = collider;
    }

    public Model(RenderInfo info, MeshInformation meshInformation) {
        this(info, meshInformation.centerPoint, meshInformation.radiusXZPlane, meshInformation.radius, meshInformation.topLeftAABB, meshInformation.botRightAABB, meshInformation.collider);
    }

}
