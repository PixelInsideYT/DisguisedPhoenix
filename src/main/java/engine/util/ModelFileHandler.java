package engine.util;

import engine.collision.Collider;
import engine.collision.CollisionShape;
import engine.collision.ConvexShape;
import graphics.loader.AssimpWrapper;
import graphics.loader.MeshInformation;
import graphics.objects.Vao;
import graphics.world.Model;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import sun.misc.IOUtils;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ModelFileHandler {

    private static Map<String, Model> alreadyLoadedModels = new HashMap<>();

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

    public static void main(String[] args) {
        JFileChooser jfc = new JFileChooser(new File("/home/linus/IdeaProjects/DisguisedPhoenix/src/main/resources/models"));
        int returnValue = jfc.showOpenDialog(null);
        while (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            int val = JOptionPane.showConfirmDialog(null, "Should a collision shape be added?");
            String colliderFileName = null;
            if (val == JOptionPane.YES_OPTION) {
                jfc.showOpenDialog(null);
                colliderFileName = jfc.getSelectedFile().getAbsolutePath();
            }
            generateModelFile(selectedFile.getPath(), colliderFileName);
            returnValue = jfc.showOpenDialog(null);
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


    private static Model loadModelFile(String name) {
        InputStream stream = ModelFileHandler.class.getClassLoader().getResourceAsStream("models/" + name);
        ByteBuffer buffer;
        Model rt = null;
        try {
            buffer = ByteBuffer.wrap(IOUtils.readAllBytes(stream)).order(ByteOrder.nativeOrder());
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
                    int axesCount = edgeAndAxeCount[c * 2 + 1];
                    Vector3f[] axesArray = new Vector3f[axesCount];
                    float[] array = new float[egdePointsCount * 4];
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
                        collider.boundingBox = shape;
                        collider.boundingBoxModel = renderAble;
                    } else {
                        collider.addCollisionShape(shape);
                    }
                }
            }

            float height=-Float.MAX_VALUE;
            float radiusXZ=-Float.MAX_VALUE;
            float radius=-Float.MAX_VALUE;

            for(int i=0;i<posAndWobble.length/4;i++){
                float x = posAndWobble[i*4];
                float y = posAndWobble[i*4+1];
                float z = posAndWobble[i*4+2];
                height=Math.max(height,y);
                radiusXZ=Math.max(radiusXZ,(float)Math.sqrt(x*x+z*z));
                radius=Math.max(radius,(float)Math.sqrt(x*x+z*z+y*y));

            }
            Vao meshVao = new Vao();
            meshVao.addDataAttributes(0, 4, posAndWobble);
            meshVao.addDataAttributes(1, 4, colorAndShininess);
            meshVao.addIndicies(indicies);
            meshVao.unbind();
            rt = new Model(meshVao,height,radiusXZ,radius, collider);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rt;
    }


    public static MeshInformation combineMeshesToOne(String modelName) {
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
        float modelHeight = Arrays.stream(model).map(m->m.height).max(Float::compare).get();
        for (MeshInformation mi:model) {
            String answer = JOptionPane.showInputDialog("How much should: " + mi.meshName + " be affected by wind?\nFormat [ 0; 3 ]:<wobble>\n0: constant wobble\n1: linear wobble from [0;radiusXZPlane]\n2: linear wobble from [0;height]\n3: linear wobble from [0;height+radiusXZPlane]");
            String[] answerSplit = answer.split(":");
            int type = Integer.parseInt(answerSplit[0]);
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
        return new MeshInformation(modelName.substring(0, modelName.lastIndexOf(".")), null, vertexPositions, colors, indicies);
    }

    public static void generateModelFile(String name, String colliderFileName) {
        MeshInformation combined = combineMeshesToOne(name);
        Collider collider = null;
        if (colliderFileName != null)
            collider = AssimpWrapper.loadCollider(colliderFileName, false);
        int headerSize = (4 + 2 * (collider != null ? collider.allTheShapes.size() + 1 : 0)) * Integer.BYTES;
        int meshSize = (combined.vertexPositions.length + combined.colors.length) * Float.BYTES + combined.indicies.length * Integer.BYTES;
        int colliderSize = calculateColliderSize(collider);
        int modelFileSize = headerSize + meshSize + colliderSize;
        ByteBuffer buffer = BufferUtils.createByteBuffer(modelFileSize);
        //write header
        buffer.putInt(combined.vertexPositions.length);
        buffer.putInt(combined.colors.length);
        buffer.putInt(combined.indicies.length);
        if (collider != null) {
            buffer.putInt(1 + collider.allTheShapes.size());
            buffer.putInt(collider.boundingBox.cornerPoints.length);
            buffer.putInt(collider.boundingBox.getAxis().length);
            for (CollisionShape cs : collider.allTheShapes) {
                buffer.putInt(cs.cornerPoints.length);
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
            for (Vector3f point : collider.boundingBox.cornerPoints) {
                buffer.putFloat(point.x);
                buffer.putFloat(point.y);
                buffer.putFloat(point.z);
            }
            for (Vector3f axe : collider.boundingBox.getAxis()) {
                buffer.putFloat(axe.x);
                buffer.putFloat(axe.y);
                buffer.putFloat(axe.z);
            }
            //write other shapes
            for (CollisionShape cs : collider.allTheShapes) {
                for (Vector3f point : cs.cornerPoints) {
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
        int boundingBoxSize = (collider.boundingBox.cornerPoints.length * vectorSize + collider.boundingBox.getAxis().length * vectorSize) * Float.BYTES;
        int collisonShapesSizes = collider.allTheShapes.stream().mapToInt(c -> (c.cornerPoints.length * vectorSize + c.getAxis().length * vectorSize) * Float.BYTES).sum();
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

}
