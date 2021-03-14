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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void render(List<Entity> entities) {
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
        //build command buffer per vao, fill matrix buffer and render
        for (Vao vao : toRender.keySet()) {
            Map<RenderInfo, List<Matrix4f>> modelMatrixMap = toRender.get(vao);
            int commandCount = modelMatrixMap.keySet().size();
            int[] newCommands = new int[commandCount * 5];
            int index = 0;
            int newEntries = modelMatrixMap.keySet().stream().mapToInt(ri -> modelMatrixMap.get(ri).size()).sum();
            int beginIndex = writeHead;
            int endIndex = (writeHead + newEntries - 1) % (bufferCount * maxInstanceCount);
            //loop around to zero to avoid blinking entities
            if (endIndex < beginIndex) {
                writeHead = 0;
                beginIndex = 0;
                endIndex = writeHead + newEntries - 1;
            }
            lockManger.waitForFence(beginIndex, endIndex);
            for (RenderInfo info : modelMatrixMap.keySet()) {
                List<Matrix4f> instances = modelMatrixMap.get(info);
                //add command
                newCommands[index++] = info.indiciesCount;
                newCommands[index++] = instances.size();
                newCommands[index++] = info.indexOffset;
                newCommands[index++] = info.vertexOffset;
                newCommands[index++] = writeHead;
                Main.inViewVerticies+=info.indiciesCount*instances.size();
                //add matricies to buffer
                for (Matrix4f m : instances) {
                    // * floatsPerInstance * 4 because its a bytebuffer so offset needs to be in bytes
                    m.get(writeHead * floatsPerInstance * 4, matrixBuffer);
                    writeHead++;
                    writeHead = writeHead % (bufferCount * maxInstanceCount);
                }
            }
            cmdBuffer.updateVbo(newCommands);
            //update command buffer
            cmdBuffer.bind();
            //render
            vao.bind();
            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, 0, commandCount, 0);
            Main.drawCalls++;
            vao.unbind();
            lockManger.addFence(beginIndex, endIndex);
        }
    }

}
