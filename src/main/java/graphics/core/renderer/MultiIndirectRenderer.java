package graphics.core.renderer;

import disuguisedphoenix.Entity;
import disuguisedphoenix.Main;
import engine.util.Maths;
import graphics.core.objects.BufferObject;
import graphics.core.objects.LockManger;
import graphics.core.objects.Vao;
import graphics.modelinfo.RenderInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL20.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;

@Slf4j
public class MultiIndirectRenderer {

    private static final int BUFFER_COUNT = 3;
    private final ByteBuffer matrixBuffer;
    private final LockManger lockManger;
    @Getter
    private BufferObject persistentMatrixVbo;
    public List<Entity> currentEntities = new ArrayList<>();
    //etwa 3 mb an grafikkarten speicher
    int floatsPerInstance = 16;
    int maxInstanceCount = 50000;
    int maxCommandCount = 50;
    BufferObject cmdBuffer;
    Map<Vao, int[]> renderCommands;
    private int writeHead;

    public MultiIndirectRenderer() {
        persistentMatrixVbo = new BufferObject(GL_ARRAY_BUFFER);
        matrixBuffer = persistentMatrixVbo.createPersistantVbo(maxInstanceCount * BUFFER_COUNT * floatsPerInstance);
        persistentMatrixVbo.unbind();
        cmdBuffer = new BufferObject(maxCommandCount * 5, GL_DRAW_INDIRECT_BUFFER, GL_STREAM_DRAW);
        cmdBuffer.unbind();
        renderCommands = new HashMap<>();
        lockManger = new LockManger();
        writeHead = 0;
    }

    public void prepareRenderer(List<Entity> entities) {
        currentEntities.clear();
        currentEntities.addAll(entities);
        //sort entities according to Vao and model (remember one vao has multiple models)
        Map<Vao, Map<RenderInfo, List<Matrix4f>>> vaoSortedEntries = new HashMap<>();
        for (Entity e : entities) {
            RenderInfo entityRenderInfo = e.getModel().getRenderInfo();
            if (entityRenderInfo.isMultiDrawCapable()) {
                Map<RenderInfo, List<Matrix4f>> instanceMap = vaoSortedEntries.computeIfAbsent(entityRenderInfo.getActualVao(), k -> new HashMap<>());
                List<Matrix4f> entityTransformation = instanceMap.computeIfAbsent(entityRenderInfo, k -> new ArrayList<>());
                entityTransformation.add(e.getTransformationMatrix());
                Main.inViewObjects++;
                Main.facesDrawn += entityRenderInfo.getIndicesCount() / 3;
            }
        }
        prepareRender(vaoSortedEntries);
    }

    public void prepareRenderer(RenderInfo renderInfo, List<Matrix4f> matrix4fList) {
        if(renderInfo.isMultiDrawCapable()) {
            Map<Vao, Map<RenderInfo, List<Matrix4f>>> vaoSortedEntries = new HashMap<>();
            Map<RenderInfo, List<Matrix4f>> map = new HashMap<>();
            map.put(renderInfo, matrix4fList);
            vaoSortedEntries.put(renderInfo.getActualVao(), map);
            prepareRender(vaoSortedEntries);
        }else{
            log.error("Model not MultiDraw capable");
        }
    }

    private void prepareRender(Map<Vao, Map<RenderInfo, List<Matrix4f>>> vaoSortedEntries) {
        renderCommands.clear();
        //build command buffer per vao, fill matrix buffer and render
        for (Vao vao : vaoSortedEntries.keySet()) {
            Map<RenderInfo, List<Matrix4f>> modelMatrixMap = vaoSortedEntries.get(vao);
            int newEntries = modelMatrixMap.keySet().stream().mapToInt(ri -> modelMatrixMap.get(ri).size()).sum();
            int beginIndex = writeHead;
            int endIndex = (int) Maths.clamp(writeHead + newEntries - 1f, 0, BUFFER_COUNT * maxInstanceCount - 1f);
            int overShooting = (writeHead + newEntries - 1) % (BUFFER_COUNT * maxInstanceCount);
            List<IndirectCommand> commands = new ArrayList<>();
            lockManger.waitForFence(beginIndex, endIndex);
            for (RenderInfo info : modelMatrixMap.keySet()) {
                List<Matrix4f> instances = modelMatrixMap.get(info);
                //add command
                int startingHead = writeHead;
                int instancesSize = 0;
                Main.inViewVerticies += info.getIndicesCount() * instances.size();
                //add matricies to buffer
                for (Matrix4f m : instances) {
                    // * floatsPerInstance * 4 because its a bytebuffer so offset needs to be in bytes
                    m.get(writeHead * floatsPerInstance * 4, matrixBuffer);
                    instancesSize++;
                    writeHead++;
                    if (writeHead >= BUFFER_COUNT * maxInstanceCount) {
                        lockManger.waitForFence(0, overShooting);
                        commands.add(new IndirectCommand(info.getIndicesCount(), instancesSize, info.getIndexOffset(), info.getVertexOffset(), startingHead));
                        lockManger.addFence(0, overShooting);
                        startingHead = writeHead = 0;
                        instancesSize = 0;
                    }
                }
                commands.add(new IndirectCommand(info.getIndicesCount(), instancesSize, info.getIndexOffset(), info.getVertexOffset(), startingHead));
            }
            renderCommands.put(vao, commands.stream().flatMapToInt(IndirectCommand::toStream).toArray());
            lockManger.addFence(beginIndex, endIndex);
        }
    }

    public void render() {
        for (Map.Entry<Vao, int[]> vaoCommand : renderCommands.entrySet()) {
            Vao vao = vaoCommand.getKey();
            cmdBuffer.updateVbo(vaoCommand.getValue());
            //update command buffer
            cmdBuffer.bind();
            //render
            vao.bind();
            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, 0, IndirectCommand.getCommandCount(vaoCommand.getValue()), 0);
            Main.drawCalls++;
            vao.unbind();
        }
    }

}
