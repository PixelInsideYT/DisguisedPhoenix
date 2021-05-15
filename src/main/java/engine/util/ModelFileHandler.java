package engine.util;

import engine.collision.Collider;
import engine.collision.CollisionShape;
import engine.collision.ConvexShape;
import graphics.loader.AssimpWrapper;
import graphics.loader.MeshInformation;
import graphics.objects.BufferObject;
import graphics.objects.Vao;
import graphics.world.Model;
import graphics.world.RenderInfo;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;

public class ModelFileHandler {

    private static final Map<String, Model> alreadyLoadedModels = new HashMap<>();

    //loads a model file
    // joins all the meshes into one
    // generates wobble data and reads per vertex color (if there is no per vertex color material color is used)
    // shininess from material is saved in alpha of color
    // attaches collider if possible

    //ModelFile: binary, first int=number of floats for positions and wobble itensity --> (3f pos, 1f wobble)*vertexcount
    // second int =number of floats for color and shininess --> (3f color, 1f shininess)*vertexcount
    // third int = number of ints for indicies
    // fourth int = number of collision shapes
    // number of collision shapes * 2 = number of egdes and normals


    //reads modelbuilder info and builts models from that description
    // modelPath
    // possible collision path
    // name wobbletype map

    private static List<ModelConfig> modelConfigs = new ArrayList<>();

    public static void main(String[] args) {
        JFileChooser jfc = new JFileChooser(new File("/home/linus/IdeaProjects/DisguisedPhoenix/src/main/resources/models"));
        int returnValue = jfc.showOpenDialog(null);
        while (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            if(selectedFile.getAbsolutePath().endsWith(".info")){
                try {
                    String content = Files.readString(selectedFile.toPath());
                    String[] modelConfigsString = content.split(">");
                    for(String mc:modelConfigsString){
                        if(mc.length()>0&&mc.split("\n").length>0){
                            ModelConfig mco = ModelConfig.load(mc);
                            generateModelFile(mco.relativePath,mco.relativeColliderPath,mco.wobbleInfo,false);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
            int val = JOptionPane.showConfirmDialog(null, "Should a collision shape be added?");
            String colliderFileName = null;
            if (val == JOptionPane.YES_OPTION) {
                jfc.showOpenDialog(null);
                colliderFileName = jfc.getSelectedFile().getAbsolutePath();
            }
            generateModelFile(selectedFile.getPath(), colliderFileName, new HashMap<>(),false);
            }
            returnValue = jfc.showOpenDialog(null);
        }
        try {
            PrintWriter pi = new PrintWriter(new File("/home/linus/IdeaProjects/DisguisedPhoenix/src/main/resources/models/ModelBuilder.info"),"UTF-8");
            for(ModelConfig ci:modelConfigs){
                ci.writeToFile(pi);
            }
            pi.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static void regenerateModels(String modelInfoFile){
        try {
            String content = Files.readString(new File(modelInfoFile).toPath());
            String[] modelConfigsString = content.split(">");
            for(String mc:modelConfigsString){
                if(mc.length()>0&&mc.split("\n").length>0){
                    ModelConfig mco = ModelConfig.load(mc);
                    generateModelFile(mco.relativePath,mco.relativeColliderPath,mco.wobbleInfo,false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Model getModel(String name) {
        Model rt = alreadyLoadedModels.get(name);
        if (rt == null) {
            System.out.println("Model " + name + " isnt loaded yet. Trying to load it");
            rt = loadModelFile(name);
            alreadyLoadedModels.put(name, rt);
        }
        return rt;
    }

    public static void loadModelsForMultiDraw(BufferObject matrixVbo, String... names) {
        int posWobbleANDColorShininessSize = 0;//both have equal size
        int indiciesSize = 0;
        List<MeshInformation> meshes = new ArrayList<>();
        for (String name : names) {
            MeshInformation info = loadModelToMeshInfo(name);
            posWobbleANDColorShininessSize += info.vertexPositions.length;
            indiciesSize += info.indicies.length;
            meshes.add(info);
        }
        float[] combinedPosAndWobble = new float[posWobbleANDColorShininessSize];
        float[] combinedColorAndShininess = new float[posWobbleANDColorShininessSize];
        int[] combinedIndicies = new int[indiciesSize];
        Vao finishedVao = new Vao();
        int indexOffset = 0;
        int vertexOffset = 0;
        for (MeshInformation info : meshes) {
            System.arraycopy(info.vertexPositions, 0, combinedPosAndWobble, vertexOffset * 4, info.vertexPositions.length);
            System.arraycopy(info.colors, 0, combinedColorAndShininess, vertexOffset * 4, info.colors.length);
            System.arraycopy(info.indicies, 0, combinedIndicies, indexOffset, info.indicies.length);
            Model model = new Model(new RenderInfo(finishedVao, info.indicies.length, indexOffset, vertexOffset), info);
            alreadyLoadedModels.put(info.meshName, model);
            indexOffset += info.indicies.length;
            vertexOffset += info.vertexPositions.length / 4;
        }
        finishedVao.addDataAttributes(0, 4, combinedPosAndWobble);
        finishedVao.addDataAttributes(1, 4, combinedColorAndShininess);
        finishedVao.addInstancedAttribute(matrixVbo, 2, 4, 16, 0);
        finishedVao.addInstancedAttribute(matrixVbo, 3, 4, 16, 4);
        finishedVao.addInstancedAttribute(matrixVbo, 4, 4, 16, 8);
        finishedVao.addInstancedAttribute(matrixVbo, 5, 4, 16, 12);
        finishedVao.addIndicies(combinedIndicies);
        finishedVao.unbind();
    }

    public static MeshInformation loadModelToMeshInfo(String name) {
        InputStream stream = ModelFileHandler.class.getClassLoader().getResourceAsStream("models/" + name);
        ByteBuffer buffer;
        try {
            buffer = ByteBuffer.wrap(readAllBytes(stream)).order(ByteOrder.nativeOrder());
            stream.close();
            int vertexFloats = buffer.getInt();
            int colorFloats = buffer.getInt();
            int indiciesCount = buffer.getInt();
            int collisionShapeCount = buffer.getInt();
            int[] edgeAndAxeCount = new int[collisionShapeCount * 2];
            for (int i = 0; i < collisionShapeCount; i++) {
                edgeAndAxeCount[i * 2] = buffer.getInt();
                edgeAndAxeCount[i * 2 + 1] = buffer.getInt();
            }
            float[] posAndWobble = new float[vertexFloats];
            float[] colorAndShininess = new float[colorFloats];
            int[] indicies = new int[indiciesCount];
            for (int i = 0; i < vertexFloats; i++) {
                posAndWobble[i] = buffer.getFloat();
            }
            for (int i = 0; i < colorFloats; i++) {
                colorAndShininess[i] = buffer.getFloat();
            }
            for (int i = 0; i < indiciesCount; i++) {
                indicies[i] = buffer.getInt();
            }
            Collider collider = null;
            if (collisionShapeCount > 0) {
                collider = new Collider();
                for (int c = 0; c < collisionShapeCount; c++) {
                    int egdePointsCount = edgeAndAxeCount[c * 2];
                    Vector3f[] edgePointsArray = new Vector3f[egdePointsCount];
                    float[] array = new float[egdePointsCount * 4];

                    int axesCount = edgeAndAxeCount[c * 2 + 1];
                    Vector3f[] axesArray = new Vector3f[axesCount];
                    for (int e = 0; e < egdePointsCount; e++) {
                        float x = buffer.getFloat();
                        float y = buffer.getFloat();
                        float z = buffer.getFloat();
                        array[e * 4] = x;
                        array[e * 4 + 1] = y;
                        array[e * 4 + 2] = z;
                        array[e * 4 + 3] = 0;
                        edgePointsArray[e] = new Vector3f(x, y, z);
                    }
                    for (int e = 0; e < axesCount; e++) {
                        float x = buffer.getFloat();
                        float y = buffer.getFloat();
                        float z = buffer.getFloat();
                        axesArray[e] = new Vector3f(x, y, z);
                    }
                    int[] indiciesCollisionShape = new int[egdePointsCount * egdePointsCount * 2];
                    int counter = 0;
                    for (int i = 0; i < egdePointsCount; i++)
                        for (int j = i; j < egdePointsCount; j++) {
                            indiciesCollisionShape[counter++] = i;
                            indiciesCollisionShape[counter++] = j;
                        }
                    Vao renderAble = new Vao();
                    renderAble.addDataAttributes(0, 4, array);
                    renderAble.addDataAttributes(1, 3, new float[array.length]);
                    renderAble.addIndicies(indiciesCollisionShape);
                    renderAble.unbind();
                    ConvexShape shape = new ConvexShape(edgePointsArray, axesArray, renderAble);
                    if (c == 0) {
                        collider.setBoundingBox(shape);
                        collider.setBoundingBoxModel(null);
                    } else {
                        collider.addCollisionShape(shape);
                    }
                }
            }

            float height = -Float.MAX_VALUE;
            float radiusXZ = -Float.MAX_VALUE;
            Vector3f relativeCenter = new Vector3f();
            Vector3f farPoint = new Vector3f();
            for (int i = 0; i < posAndWobble.length / 4; i++) {
                float x = posAndWobble[i * 4];
                float y = posAndWobble[i * 4 + 1];
                float z = posAndWobble[i * 4 + 2];
                relativeCenter.add(x, y, z);
                height = Math.max(height, y);
                radiusXZ = Math.max(radiusXZ, (float) Math.sqrt(x * x + z * z));
                farPoint.x = Math.max(farPoint.x, Math.abs(x));
                farPoint.y = Math.max(farPoint.y, Math.abs(y));
                farPoint.z = Math.max(farPoint.z, Math.abs(z));
            }
            relativeCenter.div(posAndWobble.length / 4f);
            //TODO calculate radius better
            float radius = farPoint.distance(relativeCenter);
            MeshInformation meshInformation = new MeshInformation(name, null, posAndWobble, colorAndShininess, indicies);
            meshInformation.height = height;
            meshInformation.radiusXZPlane = radiusXZ;
            meshInformation.radius = radius;
            meshInformation.centerPoint = relativeCenter;
            meshInformation.collider = collider;
            return meshInformation;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static Model loadModelFile(String name) {
        MeshInformation meshInformation = loadModelToMeshInfo(name);
        Vao vao = new Vao();
        vao.addDataAttributes(0, 4, meshInformation.vertexPositions);
        vao.addDataAttributes(1, 4, meshInformation.colors);
        vao.addIndicies(meshInformation.indicies);
        vao.unbind();
        return new Model(new RenderInfo(vao), meshInformation);
    }


    public static MeshInformation combineMeshesToOne(String modelName, Map<String, String> nameToWobbleInfoMap, boolean putZero) {
        //load model
        MeshInformation[] model = AssimpWrapper.loadModelToMeshInfo(modelName);
        int combinedVerticiesCount = 0, combinedIndiciesCount = 0;
        for (MeshInformation mi : model) {
            combinedIndiciesCount += mi.indicies.length;
            combinedVerticiesCount += mi.getVertexCount();
        }
        float[] vertexPositions = new float[combinedVerticiesCount * 4];
        float[] colors = new float[combinedVerticiesCount * 4];
        int[] indicies = new int[combinedIndiciesCount];
        //choose wobbling parts and how much they wobble
        //join meshes
        int indiciesOffset = 0;
        int indiciesArrayOffset = 0;
        int vertexArrayOffset = 0;
        float modelHeight = Arrays.stream(model).map(m -> m.height).max(Float::compare).get();
        float height = 0f;
        for (MeshInformation mi : model) {
            String answer = null;
            if (nameToWobbleInfoMap != null) answer = nameToWobbleInfoMap.get(mi.meshName);
            if (answer == null) {
                if(!putZero) {
                    answer = JOptionPane.showInputDialog("How much should: " + mi.meshName + " be affected by wind?\nFormat [ 0; 3 ]:<wobble>\n0: constant wobble\n1: linear wobble from [0;radiusXZPlane]\n2: linear wobble from [0;height]\n3: linear wobble from [0;height+radiusXZPlane]");
                }else {
                    answer="0:0";
                }

                nameToWobbleInfoMap.put(mi.meshName,answer);
            }
            String[] answerSplit = answer.split(":");
            int type = Integer.parseInt(answerSplit[0]);

            for(int i=0;i<mi.getVertexCount();i++){
                height=Math.max(height,mi.vertexPositions[i * 3 + 1]);
            }
            float maxWobble = Float.parseFloat(answerSplit[1]);
            for (int i = 0; i < mi.getVertexCount(); i++) {
                vertexPositions[vertexArrayOffset + i * 4] = mi.vertexPositions[i * 3];
                vertexPositions[vertexArrayOffset + i * 4 + 1] = mi.vertexPositions[i * 3 + 1];
                vertexPositions[vertexArrayOffset + i * 4 + 2] = mi.vertexPositions[i * 3 + 2];
                vertexPositions[vertexArrayOffset + i * 4 + 3] = Maths.clamp(calculateWobble(maxWobble, type, mi.radiusXZPlane, modelHeight, mi.vertexPositions[i * 3], mi.vertexPositions[i * 3 + 1], mi.vertexPositions[i * 3 + 2]), 0, maxWobble);
                colors[vertexArrayOffset + i * 4] = mi.colors[i * 3];
                colors[vertexArrayOffset + i * 4 + 1] = mi.colors[i * 3 + 1];
                colors[vertexArrayOffset + i * 4 + 2] = mi.colors[i * 3 + 2];
                colors[vertexArrayOffset + i * 4 + 3] = mi.meshMaterial.shininess;
            }
            vertexArrayOffset += mi.getVertexCount() * 4;
            for (int i = 0; i < mi.indicies.length; i++) {
                indicies[indiciesArrayOffset + i] = mi.indicies[i] + indiciesOffset;
            }
            indiciesArrayOffset += mi.indicies.length;
            indiciesOffset += mi.getVertexCount();
        }
        for(int i=0;i<vertexPositions.length/4;i++){
            vertexPositions[i * 4]/=height;
            vertexPositions[i * 4+1]/=height;
            vertexPositions[i * 4+2]/=height;
        }
        return new MeshInformation(modelName.substring(0, modelName.lastIndexOf(".")), null, vertexPositions, colors, indicies);
    }

    public static void generateModelFile(String name, String colliderFileName, Map<String, String> nameToWobbleMap,boolean putZero) {
        MeshInformation combined = combineMeshesToOne(name, nameToWobbleMap,putZero);
        Collider collider = null;
        if (colliderFileName != null)
            collider = AssimpWrapper.loadCollider(colliderFileName);
        int headerSize = (4 + 2 * (collider != null ? collider.getAllTheShapes().size() + 1 : 0)) * Integer.BYTES;
        int meshSize = (combined.vertexPositions.length + combined.colors.length) * Float.BYTES + combined.indicies.length * Integer.BYTES;
        int colliderSize = calculateColliderSize(collider);
        int modelFileSize = headerSize + meshSize + colliderSize;
        ByteBuffer buffer = BufferUtils.createByteBuffer(modelFileSize);
        //write header
        buffer.putInt(combined.vertexPositions.length);
        buffer.putInt(combined.colors.length);
        buffer.putInt(combined.indicies.length);
        if (collider != null) {
            buffer.putInt(1 + collider.getAllTheShapes().size());
            buffer.putInt(collider.getBoundingBox().getCornerPointCount());
            buffer.putInt(collider.getBoundingBox().getAxis().length);
            for (CollisionShape cs : collider.getAllTheShapes()) {
                buffer.putInt(cs.getCornerPointCount());
                buffer.putInt(cs.getAxis().length);
            }
        } else {
            buffer.putInt(0);
        }
        //write mesh data
        //positions and wobble
        for (float f : combined.vertexPositions) {
            buffer.putFloat(f);
        }
        //colors and shininess
        for (float f : combined.colors) {
            buffer.putFloat(f);
        }
        //indicies
        for (int i : combined.indicies) {
            buffer.putInt(i);
        }
        writeColliderToBuffer(collider, buffer);
        saveByteBuffer(buffer, name.substring(0, name.lastIndexOf(".")) + ".modelFile");
        modelConfigs.add(new ModelConfig(name,colliderFileName,nameToWobbleMap));
    }

    private static void saveByteBuffer(ByteBuffer buffer, String name) {
        File saveFile = new File(name);
        buffer.flip();
        try {
            DataOutputStream stream = new DataOutputStream(new FileOutputStream(saveFile));
            while (buffer.remaining() > 0) {
                stream.write(buffer.get());
            }
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeColliderToBuffer(Collider collider, ByteBuffer buffer) {
        if (collider != null) {
            //write boundingBox
            for (Vector3f point : collider.getBoundingBox().getCornerPoints()) {
                buffer.putFloat(point.x);
                buffer.putFloat(point.y);
                buffer.putFloat(point.z);
            }
            for (Vector3f axe : collider.getBoundingBox().getAxis()) {
                buffer.putFloat(axe.x);
                buffer.putFloat(axe.y);
                buffer.putFloat(axe.z);
            }
            //write other shapes
            for (CollisionShape cs : collider.getAllTheShapes()) {
                for (Vector3f point : cs.getCornerPoints()) {
                    buffer.putFloat(point.x);
                    buffer.putFloat(point.y);
                    buffer.putFloat(point.z);
                }
                for (Vector3f axe : cs.getAxis()) {
                    buffer.putFloat(axe.x);
                    buffer.putFloat(axe.y);
                    buffer.putFloat(axe.z);
                }
            }
        }
    }

    private static int calculateColliderSize(Collider collider) {
        if (collider == null) return 0;
        int vectorSize = 3;
        int boundingBoxSize = (collider.getBoundingBox().getCornerPointCount() * vectorSize + collider.getBoundingBox().getAxis().length * vectorSize) * Float.BYTES;
        int collisonShapesSizes = collider.getAllTheShapes().stream().mapToInt(c -> (c.getCornerPointCount() * vectorSize + c.getAxis().length * vectorSize) * Float.BYTES).sum();
        return boundingBoxSize + collisonShapesSizes;
    }


    private static float calculateWobble(float maxWobble, int type, float radius, float height, float x, float y, float z) {
        float factor = 0f;
        switch (type) {
            case 0:
                factor = 1f;
                break;
            case 1:
                factor = (float) Math.sqrt(x * x + z * z) / radius;
                break;
            case 2:
                factor = y / height;
                break;
            case 3:
                factor = (float) (Math.sqrt(x * x + z * z) + y) / (radius + height);
                break;
            default:
                System.out.println("Option: " + type + " not available");
                break;
        }
        return factor * maxWobble;
    }

    public static byte[] readAllBytes(InputStream is) throws IOException {
        int len = Integer.MAX_VALUE;
        int DEFAULT_BUFFER_SIZE = 8192;
        int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = is.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                    result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

}

class ModelConfig {

    String relativePath;
    String relativeColliderPath;
    Map<String, String> wobbleInfo;

    public ModelConfig(String relativePath, String relativeColliderPath, Map<String, String> wobbleInfo) {
        this.relativePath = relativePath;
        this.relativeColliderPath = relativeColliderPath;
        this.wobbleInfo = wobbleInfo;
    }

    public static ModelConfig load(String s) {
        int startIndex = s.startsWith("\n")?1:0;
        String[] components = s.split("\n");
        Map<String, String> wobbleInfo = new HashMap<>();
        for (String i : components[startIndex+2].split("\\|")) {
            if (i.length() > 0) {
                String[] wi = i.split("~");
               wobbleInfo.put(wi[0], wi[1]);
            }
        }
        return new ModelConfig(components[startIndex], components[startIndex+1].equals("null") ? null : components[startIndex+1], wobbleInfo);
    }

    //a file config is split by '>'
    public void writeToFile(PrintWriter out) {
        out.println(relativePath);
        out.println(relativeColliderPath != null ? relativeColliderPath : "null");
        Iterator<String> itr= wobbleInfo.keySet().iterator();
        while(itr.hasNext()){
            String key = itr.next();
            out.print(key + "~" + wobbleInfo.get(key) + (itr.hasNext()? "|":""));
        }
        out.println(">");
    }

}