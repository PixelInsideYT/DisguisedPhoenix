package graphics.core.objects;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL45.*;

public class BufferObject {

    private static final List<BufferObject> allBufferObjects = new ArrayList<>();
    private final int target;
    private int bufferID;

    public BufferObject(int target) {
        this.target = target;
        bufferID = glGenBuffers();
        bind();
        allBufferObjects.add(this);
    }

    public BufferObject(int dataCount, int target, int usage) {
        this(target);
        glBufferData(target, dataCount * 4L, usage);
    }

    public BufferObject(float[] data, int target, int usage) {
        this(target);
        glBufferData(target, data, usage);
    }

    public static void cleanUp() {
        allBufferObjects.forEach(vbo -> glDeleteBuffers(vbo.bufferID));
    }

    public ByteBuffer createPersistantVbo(int floatCount) {
        int flags = GL_MAP_PERSISTENT_BIT | GL_MAP_WRITE_BIT | GL_MAP_COHERENT_BIT;
        long floatSizeInBytes = floatCount * 4L;
        glBufferStorage(target, floatSizeInBytes, flags);
        return glMapBufferRange(target, 0, floatSizeInBytes, flags);
    }

    public void bufferData(int[] data, int usage) {
        glBufferData(target, data, usage);
    }
    public void bufferData(float[] data, int usage) {
        glBufferData(target, data, usage);
    }
    public void updateVbo(FloatBuffer data) {
        bind();
        glBufferSubData(target, 0, data);
        unbind();
    }

    public void updateVbo(int[] data) {
        bind();
        glBufferSubData(target, 0, data);
        unbind();
    }
    public void updateVbo(float[] data) {
        bind();
        glBufferSubData(target, 0, data);
        unbind();
    }
    public void bind() {
        glBindBuffer(target, bufferID);
    }

    public BufferObject unbind() {
        glBindBuffer(target, 0);
        return this;
    }

    public int getBufferID() {
        return bufferID;
    }

    public int[] getBufferContentInt(int size){
        bind();
        int[] rt = new int[size];
        glGetBufferSubData(target,0,rt);
        return rt;
    }
    public float[] getBufferContentFloat(int size){
        bind();
        float[] rt = new float[size];
        glGetBufferSubData(target,0,rt);
        return rt;
    }
    public int getTarget() {
        return target;
    }
}
