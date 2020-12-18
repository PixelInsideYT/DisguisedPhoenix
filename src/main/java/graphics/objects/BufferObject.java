package graphics.objects;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL45;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class BufferObject {

    private static final List<BufferObject> allBufferObjects = new ArrayList<>();

    public int bufferID;
    private final int target;

    public BufferObject(int target) {
        this.target = target;
        bufferID = GL15.glGenBuffers();
        bind();
        allBufferObjects.add(this);
    }

    public BufferObject(int dataCount, int target, int usage) {
        this(target);
        GL15.glBufferData(target, dataCount * 4, usage);
    }

    public BufferObject(float[] data, int target, int usage) {
        this(target);
        GL15.glBufferData(target, data, usage);
    }


    public ByteBuffer createPersistantVbo(int floatCount) {
        int flags = GL45.GL_MAP_PERSISTENT_BIT | GL45.GL_MAP_WRITE_BIT | GL45.GL_MAP_COHERENT_BIT;
        GL45.glBufferStorage(target, floatCount * 4, flags);
        return GL45.glMapBufferRange(target, 0, floatCount * 4, flags);
    }

    public static void cleanUp() {
        allBufferObjects.forEach(vbo -> GL15.glDeleteBuffers(vbo.bufferID));
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
        GL15.glBindBuffer(target, bufferID);
    }

    public BufferObject unbind() {
        GL15.glBindBuffer(target, 0);
        return this;
    }

}
