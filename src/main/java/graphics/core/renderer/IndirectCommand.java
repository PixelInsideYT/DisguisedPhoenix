package graphics.core.renderer;

import java.util.stream.IntStream;

public class IndirectCommand {

    int count;
    int instanceCount;
    int firstIndex;
    int baseVertex;
    int baseInstance;

    public IndirectCommand(int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
        this.count = count;
        this.instanceCount = instanceCount;
        this.firstIndex = firstIndex;
        this.baseVertex = baseVertex;
        this.baseInstance = baseInstance;
    }

    public static int getCommandCount(int[] intCommand) {
        return intCommand.length / 5;
    }

    public IntStream toStream() {
        return IntStream.of(count, instanceCount, firstIndex, baseVertex, baseInstance);
    }

}
