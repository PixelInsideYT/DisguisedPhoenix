package disuguisedphoenix.rendering;

import engine.world.Octree;
import graphics.core.objects.BufferObject;
import graphics.core.shaders.ComputeShader;
import graphics.core.shaders.ShaderFactory;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

public class OcclusionCalculator {

    private static final int MAX_OCCLUSION_OBJECTS=1024;
    private static final int FLOATS_PER_INSTANCE=8;

    private final BufferObject matrixBuffer;
    private final ByteBuffer mappedMemory;
    private final BufferObject resultBuffer;
    private final ComputeShader computeShader;

    public OcclusionCalculator() {
        computeShader = new ComputeShader(ShaderFactory.loadShaderCode("compute/occlusionCompute.glsl"));
        computeShader.loadUniforms("hiZ","projViewMatrix","viewPortWidth","viewPortHeight","maxSize");
        computeShader.loadBufferResource("resultBuffer",0);
        computeShader.loadBufferResource("matrixBuffer",1);
        matrixBuffer = new BufferObject(GL43.GL_SHADER_STORAGE_BUFFER);
        mappedMemory=matrixBuffer.createPersistantVbo(FLOATS_PER_INSTANCE*MAX_OCCLUSION_OBJECTS);
        resultBuffer = new BufferObject(GL43.GL_SHADER_STORAGE_BUFFER);
        resultBuffer.bufferData(new int[MAX_OCCLUSION_OBJECTS], GL_DYNAMIC_DRAW);
    }

    public List<Boolean> getVisibilityInformation(int hiZTexture, List<Octree> inFrustumOctrees, Matrix4f projViewMatrix, int width, int height) {
        List<Boolean> rt = new ArrayList<>();
        computeShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, hiZTexture);
        computeShader.bindBuffer("resultBuffer",resultBuffer);
        computeShader.bindBuffer("matrixBuffer",matrixBuffer);
        computeShader.loadMatrix4f("projViewMatrix",projViewMatrix);
        computeShader.loadFloat("viewPortWidth",width);
        computeShader.loadFloat("viewPortHeight",height);

        int dispatchCount=(int)Math.ceil(inFrustumOctrees.size()/(float)MAX_OCCLUSION_OBJECTS);
        for(int d=0;d<dispatchCount;d++) {
            int offset = d*MAX_OCCLUSION_OBJECTS;
            int invocations = writeDataToBuffer(inFrustumOctrees,offset);
            computeShader.loadInt("maxSize", invocations);
            int dispatch = (int) Math.ceil(invocations / 32f);
            computeShader.dispatch(dispatch, 1, 1);
            computeShader.setSSBOAccessBarrier();
            int[] result = resultBuffer.getBufferContentInt(invocations);

            for (int i = 0; i < invocations; i++) {
                rt.add(result[i] == 1);
            }
        }

        return rt;
    }

    private int writeDataToBuffer(List<Octree> inFrustumOctrees, int listOffset){
        int writeHead=0;
        for(int i=listOffset;i<Math.min(listOffset+MAX_OCCLUSION_OBJECTS,inFrustumOctrees.size());i++){
            // * floatsPerInstance * 4 because its a bytebuffer so offset needs to be in bytes
            Octree node = inFrustumOctrees.get(i);
            Vector3f min = node.getMin();
            Vector3f max = new Vector3f(node.getMax()).sub(min);
            min.get(writeHead * FLOATS_PER_INSTANCE * 4, mappedMemory);
            max.get((writeHead * FLOATS_PER_INSTANCE+4) * 4, mappedMemory);
            writeHead++;
        }
        return writeHead;
    }

}
