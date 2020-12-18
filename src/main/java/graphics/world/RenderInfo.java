package graphics.world;

import graphics.objects.Vao;

public class RenderInfo {

   public Vao actualVao;

   public  boolean isMultiDrawCapabel;
   public  int indiciesCount;
   public  int indexOffset;
   public  int vertexOffset;

    public RenderInfo(Vao actualVao, int indiciesCount, int indexOffset, int vertexOffset) {
        this.actualVao = actualVao;
        this.indiciesCount = indiciesCount;
        this.indexOffset = indexOffset;
        this.vertexOffset = vertexOffset;
        isMultiDrawCapabel =true;
    }

    public RenderInfo(Vao actualVao) {
        this.actualVao = actualVao;
        this.indiciesCount=actualVao.getIndiciesLength();
        isMultiDrawCapabel =false;
    }
}
