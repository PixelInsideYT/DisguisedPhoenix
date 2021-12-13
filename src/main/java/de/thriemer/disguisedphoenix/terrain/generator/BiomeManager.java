package de.thriemer.disguisedphoenix.terrain.generator;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class BiomeManager {

    BiomeInformation[] biomes;
    private static final float REFERENCE_HUMIDITY =400;
    private static final float BLENDING_DISTANCE = 0.01f;

    //TODO interpolate between
    int size = 1080 * 2;
    public BufferedImage replicatedMap = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

    public BiomeManager() {
        //load biome configs
        Gson gson = new Gson();
        biomes = gson.fromJson(loadBiomes("biomeInfo.json"), BiomeInformation[].class);
    }

    public static void main(String[] args) {
        String info = loadBiomes("biomeTemperature.csv");
        String lines[] = info.split("\n");
        List<Vector2f> points = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String[] xy = lines[i].split(";");
            points.add(new Vector2f(Float.parseFloat(xy[0]), Float.parseFloat(xy[1])));
        }
        List<BiomeInformation> infos = new ArrayList<>();
        infos.add(generate("Tundra", points, new Vector3f(0.576f,0.655f,0.675f), 1, 2, 3));
        infos.add(generate("Boreal Forest", points, new Vector3f(0.357f,0.561f,0.322f), 2, 4, 5, 6, 3));
        infos.add(generate("Woodland/Shrubland", points,  new Vector3f(0.702f,0.486f,0.024f),4, 14, 7));
        infos.add(generate("Temperate grassland/cold desert",points,  new Vector3f(0.573f,0.494f,0.188f), 2, 13, 14, 4));
        infos.add(generate("Temperate seasonal forest", points, new Vector3f(0.173f,0.537f,0.627f), 4, 7, 8, 5));
        infos.add(generate("Temperate rainforest", points, new Vector3f(0.039f,0.329f,0.427f), 5, 8, 9, 6));
        infos.add(generate("Tropical seasonal forest/savanna", points,  new Vector3f(0.592f,0.647f,0.153f),14, 12, 11, 8, 7));
        infos.add(generate("tropical rainforest", points,  new Vector3f(0.027f,0.325f,0.188f),8, 11, 10, 9));
        infos.add(generate("Subtropical desert", points,  new Vector3f(0.784f,0.443f,0.216f),13, 15, 12, 14));
        System.out.println(new Gson().toJson(infos));
    }

    private static BiomeInformation generate(String name, List<Vector2f> points,Vector3f color, int... indices) {
        BiomeInformation biomeInformation = new BiomeInformation();
        biomeInformation.ccwPolygon = new Vector2f[indices.length];
        biomeInformation.color = color;
        biomeInformation.name = name;
        for (int counter=0;counter<indices.length;counter++) {
            biomeInformation.ccwPolygon[counter]=points.get(indices[counter] - 1);
        }
        return biomeInformation;
    }

//TODO: generalize biome classification, remove duplication
    public Vector3f getColor(float temperature, float humidity) {
        List<BiomeInformation> info = new ArrayList<>(Arrays.stream(biomes).toList());
        Vector2f samplePoint = new Vector2f((temperature-WorldGenerator.minTemp)/(WorldGenerator.maxTemp-WorldGenerator.minTemp),
                humidity/REFERENCE_HUMIDITY);
        info.sort((b1, b2) -> Float.compare(b2.getBiomeDistance(samplePoint), b1.getBiomeDistance(samplePoint)));
        Vector3f color = new Vector3f(0);
        float weightDivide = 0;
        boolean useNegativeWeights = false;
        if (info.get(0).getBiomeDistance(samplePoint)-BLENDING_DISTANCE <= 0) useNegativeWeights = true;
        for (int i = 0; i < 3; i++) {
            if (useNegativeWeights) {
                weightDivide += 1f/Math.abs(info.get(i).getBiomeDistance(samplePoint));
            } else {
                weightDivide += Math.max(0, info.get(i).getBiomeDistance(samplePoint));
            }
        }
        for (int i = 0; i < 3; i++) {
            Vector3f biomeColor = new Vector3f(info.get(i).color);
            float weight;
            if (useNegativeWeights) {
                weight = 1f/Math.abs(info.get(i).getBiomeDistance(samplePoint));
            } else {
                weight = Math.max(0, info.get(i).getBiomeDistance(samplePoint));
            }
            color.add(biomeColor.mul(Math.max(0, weight / weightDivide)));
        }
        drawPoint(color, temperature, humidity);
        return info.get(0).color;
    }


    String[] getModels(float temperature, float humidity){
        List<BiomeInformation> info = new ArrayList<>(Arrays.stream(biomes).toList());
        Vector2f samplePoint = new Vector2f((temperature-WorldGenerator.minTemp)/(WorldGenerator.maxTemp-WorldGenerator.minTemp),
                humidity/REFERENCE_HUMIDITY);
        info.sort((b1, b2) -> Float.compare(b2.getBiomeDistance(samplePoint), b1.getBiomeDistance(samplePoint)));
        return info.get(0).models;
    }


    private void drawPoint(Vector3f color, float temperature, float humidity) {
        int x = (int) ((temperature + 10f) / 40f * 1080) + 540;
        int y = 1080 - (int) (humidity / 400f * 1080) + 540;
        Color c = new Color((int) (color.x * 255f), (int) (color.y * 255f), (int) (color.z * 255f));
        if (x >= 0 && x < size && y >= 0 && y < size)
            replicatedMap.setRGB(x, y, c.getRGB());
    }

    public void save() {
        try {
            ImageIO.write(replicatedMap, "PNG", new File("replicatedMap.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String loadBiomes(String name) {
        StringBuilder biomeString = new StringBuilder();
        String biomeFile = "misc/" + name;
        try {
            InputStreamReader isr = new InputStreamReader(
                    BiomeManager.class.getClassLoader().getResourceAsStream(biomeFile));
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while ((line = reader.readLine()) != null) {
                biomeString.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            log.error("Could not read file: {}", biomeFile);
            e.printStackTrace();
            System.exit(-1);
        }
        return biomeString.toString();
    }

}
