package de.thriemer.graphics.modelinfo;

import de.thriemer.graphics.core.objects.Vao;
import lombok.Getter;

@Getter
public class RenderInfo {


    private final Vao actualVao;

    private final boolean isMultiDrawCapable;
private final int indicesCount;
    private int indexOffset;
    private int vertexOffset;

    public RenderInfo(Vao actualVao, int indicesCount, int indexOffset, int vertexOffset) {
        this.actualVao = actualVao;
        this.indicesCount = indicesCount;
        this.indexOffset = indexOffset;
        this.vertexOffset = vertexOffset;
        isMultiDrawCapable = true;
    }

    public RenderInfo(Vao actualVao) {
        this.actualVao = actualVao;
        this.indicesCount = actualVao.getIndicesLength();
        isMultiDrawCapable = false;
    }
}
