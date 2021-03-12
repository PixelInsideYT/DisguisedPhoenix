package graphics.loader;

import engine.collision.Box;
import engine.collision.Collider;
import engine.collision.ConvexShape;
import graphics.objects.Vao;
import graphics.world.Material;
import graphics.world.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;

public class AssimpWrapper {

    private static final int loadFlags = Assimp.aiProcess_Triangulate;

    public static Collider loadCollider(String name) {
        Collider c = new Collider();
        AIScene scene = Assimp.aiImportFile(name, loadFlags);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) == 1 || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_VALIDATION_WARNING) == 1 || scene.mRootNode() == null) {
            System.err.println("ERROR::ASSIMP: " + Assimp.aiGetErrorString());
        }
        AINode root = scene.mRootNode();
        PointerBuffer nodePointer = root.mChildren();
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        PointerBuffer meshPointer = scene.mMeshes();
        for (int i = 0; i < root.mNumChildren(); i++) {
            AINode child = AINode.create(nodePointer.get(i));
            IntBuffer meshIDs = child.mMeshes();
            Matrix4f childTransformation = fromAssimp(child.mTransformation());
            for (int j = 0; j < child.mNumMeshes(); j++) {
                AIMesh mesh = AIMesh.create(meshPointer.get(meshIDs.get(j)));
                ConvexShape cs = meshToCollisionShape( mesh, childTransformation);
                max.max(cs.getMax());
                min.min(cs.getMin());
                c.addCollisionShape(cs);
            }
        }
        c.setBoundingBox(new Box(min, max));
        Assimp.aiReleaseImport(scene);
        return c;
    }

    public static MeshInformation[] loadModelToMeshInfo(String name) {
        AIScene scene = Assimp.aiImportFile(name, loadFlags);
        if (scene == null || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) == 1 || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_VALIDATION_WARNING) == 1 || scene.mRootNode() == null) {
            System.err.println("ERROR::ASSIMP: " + Assimp.aiGetErrorString());
        }
        PointerBuffer meshPointer = scene.mMeshes();
        int numMeshes = scene.mNumMeshes();
        int slashIndex = name.lastIndexOf("/");
        String base = "models/" + name.substring(0, Math.max(slashIndex, 0));
        PointerBuffer materialPointer = scene.mMaterials();
        MeshInformation[] rt = new MeshInformation[numMeshes];
        AINode root = scene.mRootNode();
        Matrix4f globalTransform = fromAssimp(root.mTransformation());
        System.out.println(name +" \n"+globalTransform);
        PointerBuffer nodePointer = root.mChildren();
        int arrayIndex =0;
        for (int i = 0; i < root.mNumChildren(); i++) {
            AINode child = AINode.create(nodePointer.get(i));
            IntBuffer meshIDs = child.mMeshes();
            Matrix4f childTransformation = fromAssimp(child.mTransformation()).mul(globalTransform);
            System.out.println(child.mName().dataString()+" \n"+childTransformation);
            for (int j = 0; j < child.mNumMeshes(); j++) {
                AIMesh mesh = AIMesh.create(meshPointer.get(meshIDs.get(j)));
                AIMaterial material = AIMaterial.create(materialPointer.get(mesh.mMaterialIndex()));
                rt[arrayIndex] = processMesh(mesh, processMaterial(base, material), childTransformation);
                arrayIndex++;
            }
        }
        Assimp.aiReleaseImport(scene);
        return rt;
    }


    private static Vao getVaoFromMeshInfo(MeshInformation mi) {
        Vao vao = new Vao();
        vao.addDataAttributes(0, 3, mi.vertexPositions);
        vao.addDataAttributes(1, 3, mi.colors);
        vao.addIndicies(mi.indicies);
        return vao;
    }

    private static Matrix4f fromAssimp(AIMatrix4x4 m) {
       // return new Matrix4f(m.a1(), m.a2(), m.a3(), m.a4(), m.b1(), m.b2(), m.b3(), m.b4(), m.c1(), m.c2(), m.c3(), m.c4(), m.d1(), m.d2(), m.d3(), m.d4());
        return new Matrix4f(m.a1(),m.b1(),m.c1(),m.d1(),
                m.a2(),m.b2(),m.c2(),m.d2(),
                m.a3(),m.b3(),m.c3(),m.d3(),
                m.a4(),m.b4(),m.c4(),m.d4());
    }


    private static Material processMaterial(String base, AIMaterial aiMaterial) {
        Material material = new Material(getMaterialName(aiMaterial));
        PointerBuffer pointer = aiMaterial.mProperties();
        for (int i = 0; i < aiMaterial.mNumProperties(); i++) {
            AIMaterialProperty prop = AIMaterialProperty.create(pointer.get(i));
            ByteBuffer data = prop.mData();
            switch (prop.mKey().dataString()) {
                case AI_MATKEY_SHININESS:
                    material.shininess = data.getFloat();
                    break;
                case AI_MATKEY_OPACITY:
                    material.opacity = data.getFloat();
                    break;
                case AI_MATKEY_COLOR_AMBIENT:
                    material.ambient = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    break;
                case AI_MATKEY_COLOR_DIFFUSE:
                    material.diffuse = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    break;
                case AI_MATKEY_COLOR_SPECULAR:
                    material.specular = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
                    break;
            }
        }
        IntBuffer wrapMode = BufferUtils.createIntBuffer(1);
        AIString diffusePath = AIString.calloc();

        String texturePath = getTexturePath(aiMaterial, aiTextureType_DIFFUSE, diffusePath, wrapMode);
        if (texturePath != null && texturePath.length() > 0) {
            if (AssimpWrapper.class.getClassLoader().getResourceAsStream(base + "/" + texturePath) != null) {
                material.diffuseTextureId = TextureLoader.loadTexture(base + "/" + texturePath, assimpWrapToOpenGLWrap(wrapMode.get(0)), GL11.GL_LINEAR);
            } else {
                System.out.println("Cannot find texture: " + base + "/" + texturePath);
            }
        }
        AIString normalMapPath = AIString.calloc();
        texturePath = getTexturePath(aiMaterial, aiTextureType_NORMALS, normalMapPath, wrapMode);
        if (texturePath != null && texturePath.length() > 0) {
            if (AssimpWrapper.class.getClassLoader().getResourceAsStream(base + "/" + texturePath) != null) {
                material.normalsTextureTd = TextureLoader.loadTexture(base + "/" + texturePath, assimpWrapToOpenGLWrap(wrapMode.get(0)), GL11.GL_LINEAR);
            } else {
                System.out.println("Cannot find texture: " + base + "/" + texturePath);
            }
        }

        return material;
    }

    private static int assimpWrapToOpenGLWrap(int assimpWrap) {
        switch (assimpWrap) {
            case aiTextureMapMode_Clamp:
                return GL12.GL_CLAMP_TO_EDGE;
            case aiTextureMapMode_Mirror:
                return GL14.GL_MIRRORED_REPEAT;
            case aiTextureMapMode_Wrap:
                return GL14.GL_REPEAT;
            case aiTextureMapMode_Decal:
                return GL15.GL_CLAMP_TO_BORDER;
        }
        return -1;
    }

    private static String getTexturePath(AIMaterial aiMaterial, int type, AIString path, IntBuffer wrapMode) {
        Assimp.aiGetMaterialTexture(aiMaterial, type, 0, path, null, null, null, null, wrapMode, null);
        return path.dataString();
    }

    private static MeshInformation processMesh(AIMesh mesh, Material material, Matrix4f transformation) {
        String meshName = mesh.mName().dataString();
        List<Integer> modelIndicies = new ArrayList<>();
        List<Vector3f> modelVerticies = new ArrayList<>();
        Map<Vector3f, Vector3f> vertexColors = new HashMap<>();
        AIVector3D.Buffer verticies = mesh.mVertices();
        int faceCount = mesh.mNumFaces();
        AIColor4D.Buffer colors = mesh.mColors(0);
        Vector3f alternativeColor = new Vector3f(0, 0, 0);
        if (colors == null) {
            System.err.println("CANT LOAD colors ");
            if (material != null) {
                alternativeColor = material.diffuse;
            }
        }
        for (int i = 0; i < faceCount; i++) {
            AIFace face = mesh.mFaces().get(i);
            IntBuffer indicies = face.mIndices();
            for (int j = 0; j < indicies.remaining(); j++) {
                int index = indicies.get(j);
                Vector3f pos = getVec(verticies.get(index));
                transformation.transformPosition(pos);
                if (!modelVerticies.contains(pos)) {
                    modelVerticies.add(pos);
                }
                if (colors != null) {
                    AIColor4D vertexColor = colors.get(index);
                    Vector3f sRGB = new Vector3f(vertexColor.r(), vertexColor.g(), vertexColor.b());
                    Vector3f linearRGB = new Vector3f((float) Math.pow(sRGB.x, 2.2d), (float) Math.pow(sRGB.y, 2.2d), (float) Math.pow(sRGB.z, 2.2d));
                    vertexColors.put(pos, linearRGB);
                } else {
                    vertexColors.put(pos, alternativeColor);
                }
                modelIndicies.add(modelVerticies.indexOf(pos));
            }
        }
        //  MeshOptimizer.optimize(modelVerticies,modelIndicies);
        float[] vaoVerticies = new float[modelVerticies.size() * 3];
        float[] vaoColor = new float[modelVerticies.size() * 3];
        int vertexPointer = 0;
        int colorPointer = 0;
        for (int i = 0; i < modelVerticies.size(); i++) {
            Vector3f pos = modelVerticies.get(i);
            Vector3f color = vertexColors.get(pos);
            vaoVerticies[vertexPointer++] = pos.x;
            vaoVerticies[vertexPointer++] = pos.y;
            vaoVerticies[vertexPointer++] = pos.z;
            vaoColor[colorPointer++] = color.x;
            vaoColor[colorPointer++] = color.y;
            vaoColor[colorPointer++] = color.z;
        }
        return new MeshInformation(meshName, material, vaoVerticies, vaoColor, modelIndicies.stream().mapToInt(i -> i).toArray());
    }


    private static Vector3f getVec(AIVector3D vec) {
        return new Vector3f(vec.x(), vec.y(), vec.z());
    }

    private static ConvexShape meshToCollisionShape(AIMesh mesh, Matrix4f transformation) {
        Set<Vector3f> axes = new HashSet<>();
        Set<Vector3f> cornerPoints = new HashSet<>();
        AIVector3D.Buffer verticies = mesh.mVertices();
        AIVector3D.Buffer normals = mesh.mNormals();
        AIFace.Buffer faces = mesh.mFaces();
        int faceCount = mesh.mNumFaces();
        for (int i = 0; i < faceCount; i++) {
            AIFace face = faces.get(i);
            IntBuffer indiecies = face.mIndices();
            for (int j = 0; j < indiecies.remaining(); j++) {
                int index = indiecies.get(j);
                AIVector3D normal = normals.get(index);
                axes.add(new Vector3f(normal.x(), normal.y(), normal.z()));
                AIVector3D vertex = verticies.get(index);
                Vector3f vec = new Vector3f(vertex.x(), vertex.y(), vertex.z());
                cornerPoints.add(transformation.transformPosition(vec));
            }
        }
        //remove all axis pointing in the same direction
        Iterator<Vector3f> axeItr = axes.iterator();
        while (axeItr.hasNext()) {
            Vector3f negated = new Vector3f(axeItr.next());
            negated.x = negated.x != 0 ? -negated.x : 0;
            negated.y = negated.y != 0 ? -negated.y : 0;
            negated.z = negated.z != 0 ? -negated.z : 0;
            if (axes.contains(negated)) {
                axeItr.remove();
            }
        }
        return new ConvexShape(cornerPoints.toArray(new Vector3f[0]), axes.toArray(new Vector3f[0]));
    }

    private static float[] getTextureCoords(int verticesCount, AIVector3D.Buffer buffer) {
        float[] array = new float[verticesCount * 2];
        if (buffer == null || buffer.remaining() != verticesCount) {
            Arrays.fill(array, 0f);
            return array;
        }
        for (int i = 0; i < verticesCount; i++) {
            AIVector3D vec = buffer.get(i);
            array[i * 2] = vec.x();
            array[i * 2 + 1] = vec.y();
        }
        return array;
    }

    private static String getMaterialName(AIMaterial mat) {
        AIString materialName = AIString.calloc();
        Assimp.aiGetMaterialString(mat, AI_MATKEY_NAME, 0, 0, materialName);
        return materialName.dataString();
    }

    private static boolean readMaterialFloat(final AIMaterial mat, final String key, final FloatBuffer store,
                                             final IntBuffer inOut) {
        store.clear();
        return Assimp.aiGetMaterialFloatArray(mat, key, Assimp.aiTextureType_NONE, 0, store,
                inOut) == Assimp.aiReturn_SUCCESS && inOut.get(0) == 1;
    }
}