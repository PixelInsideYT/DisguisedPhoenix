package graphics.renderer;

import disuguisedphoenix.Entity;
import disuguisedphoenix.Main;
import graphics.objects.BufferObject;
import graphics.objects.LockManger;
import graphics.objects.Vao;
import graphics.world.RenderInfo;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL40.*;

public class MultiIndirectRenderer {

    //etwa 3 mb an grafikkarten speicher
    int floatsPerInstance = 16;
    int maxInstanceCount = 50000;
    public BufferObject persistantMatrixVbo;
    private final int bufferCount = 3;
    private final ByteBuffer matrixBuffer;
    int maxCommandCount = 50;
    BufferObject cmdBuffer;
    private int writeHead;
    private final LockManger lockManger;
    Map<Vao, Map<RenderInfo, List<Matrix4f>>> toRender;


    public MultiIndirectRenderer() {
        persistantMatrixVbo = new BufferObject(GL_ARRAY_BUFFER);
        matrixBuffer = persistantMatrixVbo.createPersistantVbo(maxInstanceCount * bufferCount * floatsPerInstance);
        persistantMatrixVbo.unbind();
        cmdBuffer = new BufferObject(maxCommandCount * 5, GL_DRAW_INDIRECT_BUFFER, GL_STREAM_DRAW);
        cmdBuffer.unbind();
        toRender = new HashMap<>();
        lockManger = new LockManger();
        writeHead = 0;
    }
private int count=0;
    public void render(List<Entity> entities) {
        Map<Vao, int[]> vaoCommandMap = prepareBuffersFor(entities);
        count++;
        for (Map.Entry<Vao, int[]> e : vaoCommandMap.entrySet()) {
            int[] newCommands = e.getValue();
            Vao vao = e.getKey();
            System.out.println(newCommands.length/5);
            cmdBuffer.updateVbo(newCommands);
            //update command buffer
            cmdBuffer.bind();
            //render
            vao.bind();
            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, 0, IndirectCommand.getCommandCount(newCommands), 0);
            Main.drawCalls++;
            vao.unbind();
        }
    }


    private Map<Vao, int[]> prepareBuffersFor(List<Entity> entities) {
        //sort entities according to Vao and model (remeber one vao has multiple models)
        toRender.clear();
        for (Entity e : entities) {
            RenderInfo entityRenderInfo = e.getModel().renderInfo;
            if (entityRenderInfo.isMultiDrawCapabel) {
                Map<RenderInfo, List<Matrix4f>> instanceMap = toRender.computeIfAbsent(entityRenderInfo.actualVao, k -> new HashMap<>());
                List<Matrix4f> entityTransformation = instanceMap.computeIfAbsent(entityRenderInfo, k -> new ArrayList<>());
                entityTransformation.add(e.getTransformationMatrix());
                Main.inViewObjects++;
                Main.facesDrawn += entityRenderInfo.indiciesCount / 3;
            }
        }
        Map<Vao, int[]> rtVal = new HashMap<>();
        for (Vao vao : toRender.keySet()) {
            Map<RenderInfo, List<Matrix4f>> modelMatrixMap = toRender.get(vao);
            List<IndirectCommand> drawCommandsPerVao = new ArrayList<>();
            int newEntries = modelMatrixMap.keySet().stream().mapToInt(ri -> modelMatrixMap.get(ri).size()).sum();
            int beginIndex = writeHead;
            int endIndex = (writeHead + newEntries - 1) % (bufferCount * maxInstanceCount);
            //loop around to zero to avoid blinking entities
            lockManger.waitForFence(beginIndex, endIndex);
            for (RenderInfo info : modelMatrixMap.keySet()) {
                List<Matrix4f> instances = modelMatrixMap.get(info);
                int startWriteHead = writeHead;
                int instanceCount = 0;
                Main.inViewVerticies += info.indiciesCount * instances.size();
                //add matricies to buffer
                for (Matrix4f m : instances) {
                    // * floatsPerInstance * 4 because its a bytebuffer so offset needs to be in bytes
                    m.get(writeHead * floatsPerInstance * 4, matrixBuffer);
                    writeHead++;
                    instanceCount++;
                    if (writeHead > bufferCount * maxInstanceCount) {
                        drawCommandsPerVao.add(new IndirectCommand(info.indiciesCount, instanceCount, info.indexOffset, info.vertexOffset, startWriteHead));
                        lockManger.waitForFence(beginIndex, instances.size()-instanceCount);
                        startWriteHead = writeHead=0;
                        instanceCount = 0;
                    }
                }
                drawCommandsPerVao.add(new IndirectCommand(info.indiciesCount, instanceCount, info.indexOffset, info.vertexOffset, startWriteHead));
            }
            rtVal.put(vao, drawCommandsPerVao.stream().flatMapToInt(IndirectCommand::toStream).toArray());
            lockManger.addFence(beginIndex, endIndex);
        }
        return rtVal;
    }


}
