package graphics.objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class Vao {
    private static List<Vao> allVaos = new ArrayList<>();

    int vaoId, indiciesLength;
    List<Integer> attribNumbers = new ArrayList<>();

    String name ;
    public Vao(String name) {
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);
        allVaos.add(this);
        this.name=name;
    }

    public static void cleanUpAllVaos() {
        allVaos.forEach(Vao::cleanUp);
        Vbo.cleanUp();
    }

    public int getIndiciesLength() {
        return indiciesLength;
    }

    public void bind() {
        GL30.glBindVertexArray(vaoId);
        attribNumbers.forEach(GL20::glEnableVertexAttribArray);
    }

    public void unbind() {
        attribNumbers.forEach(GL20::glDisableVertexAttribArray);
        GL30.glBindVertexArray(0);
    }

    public void cleanUp() {
        GL30.glDeleteVertexArrays(vaoId);
    }

    public Vao addDataAttributes(int attributeNumber, int coordinateSize, float[] data) {
        Vbo vbo = new Vbo(data, GL15.GL_ARRAY_BUFFER, GL20.GL_STATIC_DRAW);
        System.err.println("vbo: "+name);
        GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);
        vbo.unbind();
        attribNumbers.add(attributeNumber);
        return this;
    }


    public Vao addIndicies(int[] indicies) {
        Vbo vbo = new Vbo(GL15.GL_ELEMENT_ARRAY_BUFFER);
        vbo.bufferData(indicies, GL15.GL_STATIC_DRAW);
        indiciesLength = indicies.length;
        return this;
    }

    public void addInstancedAttribute(Vbo vbo, int attributeNumber, int coordinateSize, int instancedDataLength, int offset) {
        vbo.bind();
        GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, instancedDataLength * 4, offset * 4);
        glVertexAttribDivisor(attributeNumber, 1);
        vbo.unbind();
        attribNumbers.add(attributeNumber);
    }

}
