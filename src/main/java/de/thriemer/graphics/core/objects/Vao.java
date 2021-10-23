package de.thriemer.graphics.core.objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class Vao {
    private static final List<Vao> allVaos = new ArrayList<>();

    int vaoId;
    int indicesLength;
    List<Integer> attribNumbers = new ArrayList<>();

    public Vao() {
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);
        allVaos.add(this);
    }

    public static void cleanUpAllVaos() {
        BufferObject.cleanUp();
        allVaos.forEach(Vao::cleanUp);
    }

    public int getIndicesLength() {
        return indicesLength;
    }

    public void bind() {
        GL30.glBindVertexArray(vaoId);
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
    }

    public void cleanUp() {
        GL30.glDeleteVertexArrays(vaoId);
    }

    public Vao addDataAttributes(int attributeNumber, int coordinateSize, float[] data) {
        BufferObject vbo = new BufferObject(data, GL15.GL_ARRAY_BUFFER, GL_STATIC_DRAW);
        glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(attributeNumber);
        vbo.unbind();
        attribNumbers.add(attributeNumber);
        return this;
    }


    public Vao addIndicies(int[] indicies) {
        BufferObject vbo = new BufferObject(GL15.GL_ELEMENT_ARRAY_BUFFER);
        vbo.bufferData(indicies, GL15.GL_STATIC_DRAW);
        indicesLength = indicies.length;
        return this;
    }

    public void addInstancedAttribute(BufferObject vbo, int attributeNumber, int coordinateSize, int instancedDataLength, int offset) {
        vbo.bind();
        glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, instancedDataLength * 4, offset * 4L);
        glVertexAttribDivisor(attributeNumber, 1);
        glEnableVertexAttribArray(attributeNumber);
        vbo.unbind();
        attribNumbers.add(attributeNumber);
    }

}
