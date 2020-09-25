package graphics.objects;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Vbo {

    private static List<Vbo> allVbos = new ArrayList<>();

    public int vboID;
    private int target;

    public Vbo(int target) {
        this.target = target;
        vboID = GL15.glGenBuffers();
        bind();
        allVbos.add(this);
    }

    public Vbo(int dataCount, int target, int usage) {
        this(target);
        GL15.glBufferData(target, dataCount * 4, usage);
    }

    public Vbo(float[] data, int target, int usage) {
        this(target);
        GL15.glBufferData(target, data, usage);
    }


    public ByteBuffer createPersistantVbo(int floatCount) {
        int flags = GL45.GL_MAP_PERSISTENT_BIT | GL45.GL_MAP_WRITE_BIT | GL45.GL_MAP_COHERENT_BIT;
        GL45.glBufferStorage(target, floatCount * 4, flags);
        return GL45.glMapBufferRange(target, 0, floatCount * 4, flags);
    }

    public static void cleanUp() {
        allVbos.forEach(vbo -> GL15.glDeleteBuffers(vbo.vboID));
    }

    public void bufferData(int[] data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    public void updateVbo(FloatBuffer data) {
        bind();
        GL15.glBufferSubData(target, 0, data);
        unbind();
    }

    public void updateVbo(int[] data) {
        bind();
        GL15.glBufferSubData(target, 0, data);
        unbind();
    }

    public void bind() {
        GL15.glBindBuffer(target, vboID);
    }

    public Vbo unbind() {
        GL15.glBindBuffer(target, 0);
        return this;
    }

}
