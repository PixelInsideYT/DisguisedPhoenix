package resourceUtil;

import disuguisedphoenix.adder.GrowState;
import engine.util.ModelFileHandler;
import graphics.particles.ParticleManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RecursiveModelConverter {

    public static List<String> modelNames = new ArrayList<>();

    private static String[] exclude = new String[]{"birb", "lightPentagon", "cube"};

    public static void main(String[] args) {
        fillModelNameList(new File("/home/linus/IdeaProjects/DisguisedPhoenix/"));
        modelNames.forEach(s-> ModelFileHandler.generateModelFile(s,null,new HashMap<>(),true));
    }
    private static void fillModelNameList(File startDir) {
        File[] faFiles = startDir.listFiles();
        for (File file : faFiles) {
            if (file.getName().endsWith(".obj")||file.getName().endsWith(".fbx")) {
                boolean isBlocked = false;
                for (String s : exclude) {
                    if (file.getName().contains(s)) isBlocked = true;
                }
                String absPath = file.getAbsolutePath();
                String filename = absPath.substring(absPath.indexOf("models/") + "models/".length());
                if (!isBlocked) {
                    modelNames.add(filename);
                }
            }
            if (file.isDirectory()) {
                fillModelNameList(file);
            }
        }
    }


}
