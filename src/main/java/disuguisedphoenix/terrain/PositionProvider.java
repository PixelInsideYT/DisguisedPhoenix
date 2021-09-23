package disuguisedphoenix.terrain;

import disuguisedphoenix.terrain.generator.TerrainTriangle;
import org.joml.Vector3f;
import org.lwjgl.system.CallbackI;

import java.util.Arrays;
import java.util.Random;
import java.util.function.UnaryOperator;

public class PositionProvider {

    Random random=new Random();
    TerrainTriangle terrainTriangle;

    public PositionProvider(TerrainTriangle terrainTriangle, UnaryOperator<Vector3f> noiseFunction) {
        this.terrainTriangle = new TerrainTriangle(Arrays.stream(terrainTriangle.vecs).map(e->noiseFunction.apply(new Vector3f(e))).toArray(Vector3f[]::new));
    }

    public Vector3f getRandomPosition(){
        Vector3f randVec = new Vector3f(random.nextFloat(),random.nextFloat(),random.nextFloat());
        float manhattenDistance = randVec.x+randVec.y+randVec.z;
        randVec.mul(1f/manhattenDistance);

        return new Vector3f(terrainTriangle.vecs[0])
                .add(new Vector3f(terrainTriangle.vecs[1]).sub(terrainTriangle.vecs[0]).mul(randVec.y))
                .add(new Vector3f(terrainTriangle.vecs[2]).sub(terrainTriangle.vecs[0]).mul(randVec.z));
    }

    public float getArea(){
        return 1f/2f*(new Vector3f(terrainTriangle.vecs[0]).sub(terrainTriangle.vecs[1])
                .cross(new Vector3f(terrainTriangle.vecs[2]).sub(terrainTriangle.vecs[1]))
                .length());
    }

}
