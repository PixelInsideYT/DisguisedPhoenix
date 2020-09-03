package disuguisedPhoenix.terrain;

import engine.util.Maths;
import graphics.objects.Vao;
import graphics.world.Model;
import graphics.world.RenderInfo;
import org.joml.Matrix4f;
import org.joml.SimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Island {

    public static final int octaves = 5;
    public static final float fallOff = 0.3f;
    public static final float NOISE_SCALE = 0.0001f;
    public static final float VERTEX_PER_SIZE = 16f / 5000f;
    public static final float TERRAIN_HEIGHT = 2500f;

    private static final Vector3f terrainColorSrgb = new Vector3f(0.278f, 0.965f, 0.255f);
    private static final Vector3f terrainColor = gammaCorrect(terrainColorSrgb);

    public Model model;
    public Matrix4f transformation;
    public Vector3f position;
    private float size;
    private int vertexCount;
    private float[][] heights;
    private float[][] bottems;

    public Island(Vector3f position, float size) {
        this.position = position;
        this.size = size;
        vertexCount = (int) Math.floor(Math.max(4, VERTEX_PER_SIZE * size));
        model = createTerrain();
        transformation = new Matrix4f();
        transformation.translate(position);
    }

    private static float barryCentric(Vector3f p1, Vector3f p2, Vector3f p3, Vector2f pos) {
        float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
        float l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
        float l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
        float l3 = 1.0f - l1 - l2;
        return l1 * p1.y + l2 * p2.y + l3 * p3.y;
    }

    private static Vector3f gammaCorrect(Vector3f inColor) {
        return new Vector3f((float) Math.pow(inColor.x, 2.2d), (float) Math.pow(inColor.y, 2.2d), (float) Math.pow(inColor.z, 2.2d));
    }

    public float getSize() {
        return size;
    }

    public float getHeightOfTerrain(float worldX, float worldY, float worldZ) {
        return getBaryCentricAnswer(heights, -Float.MAX_VALUE, worldX, worldY, worldZ);
    }

    public float getBottemOfTerrain(float worldX, float worldY, float worldZ) {
        return getBaryCentricAnswer(bottems, Float.MAX_VALUE, worldX, worldY, worldZ);
    }

    private float getBaryCentricAnswer(float[][] toInterpolate, float outsideAnswer, float worldX, float worldY, float worldZ) {
        float terrainX = worldX - this.position.x;
        float terrainZ = worldZ - this.position.z;
        float gridSquareSize = size / vertexCount;
        int gridX = (int) Math.floor(terrainX / gridSquareSize);
        int gridZ = (int) Math.floor(terrainZ / gridSquareSize);
        if (gridX >= toInterpolate.length - 1 || gridZ >= toInterpolate.length - 1 || gridX < 0 || gridZ < 0 || worldY - this.position.y < -TERRAIN_HEIGHT) {
            return outsideAnswer;
        }
        float xCoord = (terrainX % gridSquareSize) / gridSquareSize;
        float zCoord = (terrainZ % gridSquareSize) / gridSquareSize;
        float answer;
        if (xCoord <= (1 - zCoord)) {
            answer = barryCentric(new Vector3f(0, toInterpolate[gridX][gridZ], 0), new Vector3f(1, toInterpolate[gridX + 1][gridZ], 0), new Vector3f(0, toInterpolate[gridX][gridZ + 1], 1), new Vector2f(xCoord, zCoord));
        } else {
            answer = barryCentric(new Vector3f(1, toInterpolate[gridX + 1][gridZ], 0), new Vector3f(1, toInterpolate[gridX + 1][gridZ + 1], 1), new Vector3f(0, toInterpolate[gridX][gridZ + 1], 1), new Vector2f(xCoord, zCoord));
        }
        return answer + this.position.y;
    }

   /* private Vector3f calculateNormal(int x, int z) {
        float heightL = getHeight(x - 1, z);
        float heightR = getHeight(x + 1, z);
        float heightD = getHeight(x, z - 1);
        float heightU = getHeight(x, z + 1);

        Vector3f normal = new Vector3f(heightL - heightR, 2f, heightD - heightU);
        normal.normalize();
        return normal;

    }*/

    private Model createTerrain() {
        heights = new float[vertexCount][vertexCount];
        bottems = new float[vertexCount][vertexCount];
        int count = vertexCount * vertexCount * 2;
        int botBeginn = vertexCount * vertexCount;
        int bottemOffset = botBeginn * 4;
        float sizeMultiplier = size / vertexCount;
        float[] verticies = new float[count * 4];
        float[] colors = new float[count * 4];
        int[] indicies = new int[12 * (vertexCount - 1) * (vertexCount - 1) + (vertexCount - 1) * 24];
        int pointer = 0;
        Vector3f relativeCenter = new Vector3f();
        Vector3f farPoint = new Vector3f();
        float height = -Float.MAX_VALUE;
        float radiusXZ = -Float.MAX_VALUE;

        for (int z = 0; z < vertexCount; z++) {
            for (int x = 0; x < vertexCount; x++) {
                int pos = pointer * 4;

                verticies[pos] = x * sizeMultiplier;
                verticies[pos + 1] = heights[x][z] = getHeight(x * size / (float) vertexCount + position.x, z * size / (float) vertexCount + position.z, x, z);
                verticies[pos + 2] = z * sizeMultiplier;
                verticies[pos + 3] = 0f;

                float modelX = verticies[pos];
                float modelY = verticies[pos + 1];
                float modelZ = verticies[pos + 2];
                relativeCenter.add(modelX, modelY, modelZ);
                height = Math.max(height, modelY);
                radiusXZ = Math.max(radiusXZ, (float) Math.sqrt(modelX * modelX + modelZ * modelZ));
                farPoint.x = Math.max(farPoint.x, Math.abs(modelX));
                farPoint.y = Math.max(farPoint.y, Math.abs(modelY));
                farPoint.z = Math.max(farPoint.z, Math.abs(modelZ));

                verticies[pos + bottemOffset] = x * sizeMultiplier;
                verticies[pos + 1 + bottemOffset] = bottems[x][z]= getIslandBottem(x * size / (float) vertexCount + position.x, z * size / (float) vertexCount + position.z, x, z);
                verticies[pos + 2 + bottemOffset]  = z * sizeMultiplier;
                verticies[pos + 3 + bottemOffset] = 0f;
                modelX = verticies[pos + bottemOffset];
                modelY = verticies[pos + 1] + bottemOffset;
                modelZ = verticies[pos + 2 + bottemOffset];
                relativeCenter.add(modelX, modelY, modelZ);
                height = Math.max(height, modelY);
                radiusXZ = Math.max(radiusXZ, (float) Math.sqrt(modelX * modelX + modelZ * modelZ));
                farPoint.x = Math.max(farPoint.x, Math.abs(modelX));
                farPoint.y = Math.max(farPoint.y, Math.abs(modelY));
                farPoint.z = Math.max(farPoint.z, Math.abs(modelZ));

                colors[pos] = terrainColor.x;
                colors[pos + 1] = terrainColor.y;
                colors[pos + 2] = terrainColor.z;
                colors[pos + 3] = 10000;

                colors[pos + bottemOffset] = 1f;
                colors[pos + 1 + bottemOffset] = 0.9f;
                colors[pos + 2 + bottemOffset] = 0.8f;
                colors[pos + 3 + bottemOffset] = 10000;

                pointer++;
            }
        }
        pointer = 0;
        for (int z = 0; z < vertexCount - 1; z++) {
            for (int x = 0; x < vertexCount - 1; x++) {
                int topLeft = x + z * vertexCount;
                int topRight = topLeft + 1;
                int botLeft = topLeft + vertexCount;
                int botRight = topRight + vertexCount;
                indicies[pointer++] = topLeft;
                indicies[pointer++] = botLeft;
                indicies[pointer++] = topRight;
                indicies[pointer++] = topRight;
                indicies[pointer++] = botLeft;
                indicies[pointer++] = botRight;

                indicies[pointer++] = botLeft + botBeginn;
                indicies[pointer++] = topLeft + botBeginn;
                indicies[pointer++] = topRight + botBeginn;
                indicies[pointer++] = botLeft + botBeginn;
                indicies[pointer++] = topRight + botBeginn;
                indicies[pointer++] = botRight + botBeginn;

                if (x == 0) {
                    indicies[pointer++] = botLeft;
                    indicies[pointer++] = topLeft + botBeginn;
                    indicies[pointer++] = botLeft + botBeginn;
                    indicies[pointer++] = topLeft;
                    indicies[pointer++] = topLeft + botBeginn;
                    indicies[pointer++] = botLeft;
                }

                if (z == 0) {
                    indicies[pointer++] = topRight;
                    indicies[pointer++] = topRight + botBeginn;
                    indicies[pointer++] = topLeft + botBeginn;
                    indicies[pointer++] = topLeft;
                    indicies[pointer++] = topRight;
                    indicies[pointer++] = topLeft + botBeginn;
                }
                if (x == vertexCount - 2) {
                    topLeft = (x + 1) + z * vertexCount;
                    botLeft = topLeft + vertexCount;
                    indicies[pointer++] = topLeft + botBeginn;
                    indicies[pointer++] = botLeft;
                    indicies[pointer++] = botLeft + botBeginn;
                    indicies[pointer++] = topLeft + botBeginn;
                    indicies[pointer++] = topLeft;
                    indicies[pointer++] = botLeft;
                }
                if (z == vertexCount - 2) {
                    topLeft = x + (z + 1) * vertexCount;
                    topRight = topLeft + 1;
                    indicies[pointer++] = topRight + botBeginn;
                    indicies[pointer++] = topRight;
                    indicies[pointer++] = topLeft + botBeginn;
                    indicies[pointer++] = topRight;
                    indicies[pointer++] = topLeft;
                    indicies[pointer++] = topLeft + botBeginn;
                }


            }
        }
        relativeCenter.div(count);
        float radius = farPoint.distance(relativeCenter);
        Vao vao = new Vao("terrain ");
        vao.addDataAttributes(0, 4, verticies);
        vao.addDataAttributes(1, 4, colors);
        vao.addIndicies(indicies);
        vao.unbind();
        return new Model(new RenderInfo(vao), relativeCenter, height, radiusXZ, radius);
    }

    private float getIslandBottem(float x, float z, int gridX, int gridZ) {
        float result = 0;
        for (int i = 0; i < octaves; i++) {
            float heightFactor = (float) Math.pow(fallOff, i);
            float octaveFactor = (float) Math.pow(2f, i);
            result -= (SimplexNoise.noise(x * NOISE_SCALE * octaveFactor, z * NOISE_SCALE * octaveFactor) + 1f) * TERRAIN_HEIGHT * heightFactor + 100;
        }
        float v2f = vertexCount / 2f;
        float dx = v2f - gridX;
        float dz = v2f - gridZ;
        float distanceFromCenter = (float) Math.sqrt(dx * dx + dz * dz);
        return heights[gridX][gridZ] - 100;
    }

    private float getHeight(float x, float z, int gridX, int gridZ) {
        float result = 0;
        for (int i = 0; i < octaves; i++) {
            float heightFactor = (float) Math.pow(fallOff, i);
            float octaveFactor = (float) Math.pow(2f, i);
            result += SimplexNoise.noise(x * NOISE_SCALE * octaveFactor, z * NOISE_SCALE * octaveFactor) * TERRAIN_HEIGHT * heightFactor;
        }
        float v2f = vertexCount / 2f;
        float radius = (float) Math.sqrt(2f * Math.pow(v2f, 2));
        float dx = v2f - gridX;
        float dz = v2f - gridZ;
        float distanceFromCenter = (float) Math.sqrt(dx * dx + dz * dz);
        return result * Maths.clamp(1f - distanceFromCenter / radius, 0, 1);
    }

    private Vector3f getColor(int x, int z) {
        return terrainColor;
    }

}
