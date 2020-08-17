package disuguisedPhoenix.terrain;

import graphics.world.Model;
import graphics.objects.Vao;
import org.joml.SimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Terrain {

    public static final float SIZE = 10000f;
    public static final float TERRAIN_HEIGHT = 1000f;

    private static final int VERTEX_COUNT = 32;
    private static final Vector3f terrainColorSrgb = new Vector3f(0.278f,0.965f,0.255f);
   private static final Vector3f terrainColor = new Vector3f((float)Math.pow(terrainColorSrgb.x,2.2d),(float)Math.pow(terrainColorSrgb.y,2.2d),(float)Math.pow(terrainColorSrgb.z,2.2d));

    public Vector3f position;
    public Model model;
    private float[][] heights;

    public Terrain(Vector3f position){
        this.position=position;
        model = createTerrain();
    }

    public float getHeightOfTerrain(float worldX, float worldZ) {
        float terrainX = worldX - this.position.x;
        float terrainZ = worldZ - this.position.z;
        float gridSquareSize = SIZE / ((float) heights.length - 1);
        int gridX = (int) Math.floor(terrainX / gridSquareSize);
        int gridZ = (int) Math.floor(terrainZ / gridSquareSize);
        if (gridX >= heights.length - 1 || gridZ >= heights.length - 1 || gridX < 0 || gridZ < 0) {
            return 0;
        }
        float xCoord = (terrainX % gridSquareSize) / gridSquareSize;
        float zCoord = (terrainZ % gridSquareSize) / gridSquareSize;
        float answer;
        if (xCoord <= (1 - zCoord)) {
            answer = barryCentric(new Vector3f(0, heights[gridX][gridZ], 0), new Vector3f(1, heights[gridX + 1][gridZ], 0), new Vector3f(0, heights[gridX][gridZ + 1], 1), new Vector2f(xCoord, zCoord));
        } else {
            answer = barryCentric(new Vector3f(1, heights[gridX + 1][gridZ], 0), new Vector3f(1, heights[gridX + 1][gridZ + 1], 1), new Vector3f(0, heights[gridX][gridZ + 1], 1), new Vector2f(xCoord, zCoord));
        }
        return answer;
    }

    private Model createTerrain() {
        heights = new float[VERTEX_COUNT][VERTEX_COUNT];
        int count = VERTEX_COUNT * VERTEX_COUNT;
        float[] vertices = new float[count * 4];
        float[] colors = new float[count * 4];
        int[] indices = new int[6 * (VERTEX_COUNT - 1) * VERTEX_COUNT];
        int vertexPointer = 0;
        for (int i = 0; i < VERTEX_COUNT; i++) {
            for (int j = 0; j < VERTEX_COUNT; j++) {
                vertices[vertexPointer * 4] = (float) j / ((float) VERTEX_COUNT - 1) * SIZE;
                float height = getHeight(j, i);
                heights[j][i] = height;
                vertices[vertexPointer * 4 + 1] = height;
                vertices[vertexPointer * 4 + 2] = (float) i / ((float) VERTEX_COUNT - 1) * SIZE;
                //wobble
                vertices[vertexPointer * 4 + 3] = 0f;

                colors[vertexPointer * 4] = terrainColor.x;
                colors[vertexPointer * 4 + 1] = terrainColor.y;
                colors[vertexPointer * 4 + 2] = terrainColor.z;
                //shininess
                colors[vertexPointer * 4 + 3] =  1080;
                vertexPointer++;
            }
        }
        int pointer = 0;
        for (int gz = 0; gz < VERTEX_COUNT - 1; gz++) {
            for (int gx = 0; gx < VERTEX_COUNT - 1; gx++) {
                int topLeft = (gz * VERTEX_COUNT) + gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz + 1) * VERTEX_COUNT) + gx;
                int bottomRight = bottomLeft + 1;
                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;
                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }
        Vao rt = new Vao();
        rt.addDataAttributes(0, 4, vertices);
        rt.addDataAttributes(1, 4, colors);
        rt.addIndicies(indices);
        rt.unbind();
        return new Model(new Vao[]{rt},TERRAIN_HEIGHT,SIZE/2f);
    }

    private static float barryCentric(Vector3f p1, Vector3f p2, Vector3f p3, Vector2f pos) {
        float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
        float l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
        float l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
        float l3 = 1.0f - l1 - l2;
        return l1 * p1.y + l2 * p2.y + l3 * p3.y;
    }

    private Vector3f calculateNormal(int x, int z){
        float heightL = getHeight(x-1, z);
        float heightR = getHeight(x+1, z);
        float heightD = getHeight(x, z-1);
        float heightU = getHeight(x, z+1);

        Vector3f normal = new Vector3f(heightL-heightR, 2f, heightD-heightU);
        normal.normalize();
        return normal;

    }

    private float getHeight(int x, int z){
        return SimplexNoise.noise(x/500f*VERTEX_COUNT,z/500f*VERTEX_COUNT)*TERRAIN_HEIGHT;
    }

}
