package graphics.renderer;

import disuguisedPhoenix.Entity;
import disuguisedPhoenix.Main;
import graphics.objects.Vao;
import graphics.objects.Vbo;
import graphics.world.RenderInfo;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiIndirectRenderer {

    //etwa 3 mb an grafikkarten speicher
    int maxInstanceCount = 50000;
    public Vbo matrixVbo;
    FloatBuffer matrixBuffer;

    int maxCommandCount = 50;
    Vbo cmdBuffer;

    Map<Vao, Map<RenderInfo, List<Matrix4f>>> toRender;


    public MultiIndirectRenderer() {
        matrixBuffer = BufferUtils.createFloatBuffer(maxInstanceCount * 16);
        matrixVbo = new Vbo(maxInstanceCount * 16, GL20.GL_ARRAY_BUFFER, GL20.GL_STREAM_DRAW);
        matrixVbo.unbind();
        cmdBuffer = new Vbo(maxCommandCount * 5, GL40.GL_DRAW_INDIRECT_BUFFER, GL20.GL_STREAM_DRAW);
        cmdBuffer.unbind();
        toRender = new HashMap<>();
    }

    public void render(List<Entity> entities) {
        //sort entities according to Vao and model (remeber one vao has multiple models)
        toRender.clear();
        for (Entity e : entities) {
            RenderInfo entityRenderInfo = e.getModel().renderInfo;
            if(entityRenderInfo.isMultiDrawCapabel){
            Map<RenderInfo, List<Matrix4f>> instanceMap = toRender.computeIfAbsent(entityRenderInfo.actualVao, k -> new HashMap<>());
            List<Matrix4f> entityTransformation = instanceMap.computeIfAbsent(entityRenderInfo, k -> new ArrayList<>());
            entityTransformation.add(e.getTransformationMatrix());
                Main.drawCalls++;
                Main.facesDrawn+=entityRenderInfo.indiciesCount/3;
            }
        }
        //build command buffer per vao, fill matrix buffer and render
        for (Vao vao : toRender.keySet()) {
            matrixBuffer.clear();
            Map<RenderInfo, List<Matrix4f>> modelMatrixMap = toRender.get(vao);
            int parameterOffset = 0;
            int commandCount = modelMatrixMap.keySet().size();
            int[] newCommands = new int[commandCount * 5];
            int index = 0;
            for (RenderInfo info : modelMatrixMap.keySet()) {
                List<Matrix4f> instances = modelMatrixMap.get(info);
                //add command
                newCommands[index++] = info.indiciesCount;
                newCommands[index++] = instances.size();
                newCommands[index++] = info.indexOffset;
                newCommands[index++] = info.vertexOffset;
                newCommands[index++] = parameterOffset;
                //add matricies to buffer
                for (Matrix4f m : instances) {
                    m.get(parameterOffset * 16, matrixBuffer);
                    parameterOffset++;
                }
            }
            matrixVbo.updateVbo(matrixBuffer);
            cmdBuffer.updateVbo(newCommands);
            //update command buffer
            cmdBuffer.bind();
            //render
            vao.bind();
            GL43.glMultiDrawElementsIndirect(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT, 0, commandCount, 0);
            vao.unbind();
        }
    }

}
