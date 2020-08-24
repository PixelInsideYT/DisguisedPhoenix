package graphics.objects;

import org.lwjgl.opengl.GL15;

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
    }

    public Vbo(int floatCount, int target, int usage) {
        this(target);
        GL15.glBufferData(target, floatCount * 4, usage);
        allVbos.add(this);
    }

    public Vbo(float[] data, int target, int usage) {
        this(target);
        bind();
        GL15.glBufferData(target, data, usage);
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

    public void bind() {
        GL15.glBindBuffer(target, vboID);
    }

    public Vbo unbind() {
        GL15.glBindBuffer(target, 0);
        return this;
    }

}
