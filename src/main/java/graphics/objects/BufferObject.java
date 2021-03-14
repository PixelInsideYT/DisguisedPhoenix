package graphics.objects;

import org.lwjgl.opengl.GL15;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

public class BufferObject {

    private static final List<BufferObject> allBufferObjects = new ArrayList<>();

    private int bufferID;
    private final int target;

    public BufferObject(int target) {
        this.target = target;
        bufferID = GL15.glGenBuffers();
        bind();
        allBufferObjects.add(this);
    }

    public BufferObject(int dataCount, int target, int usage) {
        this(target);
        GL15.glBufferData(target, dataCount * 4L, usage);
    }

    public BufferObject(float[] data, int target, int usage) {
        this(target);
        GL15.glBufferData(target, data, usage);
    }


    public ByteBuffer createPersistantVbo(int floatCount) {
        int flags = GL_MAP_PERSISTENT_BIT | GL_MAP_WRITE_BIT | GL_MAP_COHERENT_BIT;
        long floatSizeInBytes = floatCount*4L;
        glBufferStorage(target, floatSizeInBytes, flags);
        return glMapBufferRange(target, 0, floatSizeInBytes, flags);
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

    public int getBufferID(){
        return bufferID;
    }

}
