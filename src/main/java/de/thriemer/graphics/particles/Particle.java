package de.thriemer.graphics.particles;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Particle {

    private final Matrix4f transformationMatrix;
    private final Vector3f velocity;
    private final float rotationVelocity;
    private final float scaleStart;
    private final float scaleEnd;
    private final Vector4f color;
    private final Vector4f startColor;
    private final Vector4f endColor;
    private final float lifeLength;
    protected Vector3f position;
    private float rotation = (float) Math.random() * 6f;
    private float scale;
    private float lifePercentage;
    private float lived;

    public Particle(Vector3f position, Vector3f velocity, float rotationVelocity, float scaleStart, float scaleEnd, Vector4f startColor, Vector4f endColor, float lifeLength) {
        this.position = position;
        this.velocity = velocity;
        this.scale = scaleStart;
        this.scaleStart = scaleStart;
        this.scaleEnd = scaleEnd;
        this.startColor = startColor;
        this.endColor = endColor;
        this.rotationVelocity = rotationVelocity;
        transformationMatrix = new Matrix4f();
        this.lifeLength = lifeLength;
        color = new Vector4f();
    }

    public boolean update(float dt) {
        this.lifePercentage = lived / lifeLength;
        this.scale = scaleStart * (1f - lifePercentage) + scaleEnd * lifePercentage;
        this.rotation += rotationVelocity * dt;
        position.add(new Vector3f(velocity).mul(dt));
        lived += dt;
        return lived > lifeLength;
    }

    public Vector4f getColor() {
        return color.set(startColor).lerp(endColor, lifePercentage);
    }

    public Matrix4f getTransformationMatrix(Matrix4f transposeViewMatrix) {
        transformationMatrix.identity();
        transformationMatrix.translate(position);
        transformationMatrix.set3x3(transposeViewMatrix);
        transformationMatrix.rotateZ(rotation);
        transformationMatrix.scale(scale);
        return transformationMatrix;
    }

    public Vector4f getTransformation() {
        return new Vector4f(position, rotation);
    }

}
