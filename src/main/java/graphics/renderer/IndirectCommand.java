package graphics.renderer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IndirectCommand {

    int  count;
    int  instanceCount;
    int  firstIndex;
    int  baseVertex;
    int  baseInstance;

    public IndirectCommand(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
        this.count = count;
        this.instanceCount = instanceCount;
        this.firstIndex = firstIndex;
        this.baseVertex = baseVertex;
        this.baseInstance = baseInstance;
    }

    public IntStream toStream(){
        return IntStream.of(count,instanceCount,firstIndex,baseVertex,baseInstance);
    }

    public static int getCommandCount(int[] intCommand){
        return intCommand.length/5;
    }

}
