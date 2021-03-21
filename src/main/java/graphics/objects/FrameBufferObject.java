package graphics.objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public class FrameBufferObject {

    private static final List<FrameBufferObject> allFbos = new ArrayList<>();

    private int width;
    private int height;

    private final int fbo;
    private final List<Integer> colorTextures = new ArrayList<>();
    private boolean hasDepthAttachment = false;
    private boolean hasDepthTexture = false;
    private boolean isMipMappedDepth = false;
    private int depthPointer;

    public FrameBufferObject(int width, int height, int attachmentCount) {
        fbo = createFrameBuffer(attachmentCount);
        this.width = width;
        this.height = height;
        allFbos.add(this);
    }

    public FrameBufferObject addTextureAttachment(int attachment) {
        return this.addTextureAttachment(GL_RGBA8, GL_UNSIGNED_BYTE, GL_RGBA, attachment);
    }

     public FrameBufferObject addUnclampedTexture(int attachment){
         return this.addTextureAttachment(GL_RGBA16F, GL_FLOAT, GL_RGBA, attachment);
     }

    private FrameBufferObject addTextureAttachment(int type, int precision, int format, int attachment) {
        int newTexture = createTextureAttachment(type, precision, format, GL_COLOR_ATTACHMENT0 + attachment);
        colorTextures.add(newTexture);
        return this;
    }

    public FrameBufferObject addDepthBuffer() {
        this.hasDepthAttachment = true;
        this.hasDepthTexture = false;
        this.depthPointer = createDepthBufferAttachment();
        return this;
    }

    public FrameBufferObject addDepthTextureAttachment(boolean mipMapped) {
        this.hasDepthAttachment = true;
        this.hasDepthTexture = true;
        this.depthPointer = createDepthTextureAttachment(mipMapped);
        return this;
    }

    public void blitToFbo(FrameBufferObject out) {
        blit(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT, out.fbo, out.getBufferWidth(), out.getBufferHeight());
    }

    public void blitToScreen(int width, int height) {
        blit(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT, 0, width, height);
    }

    public void blitDepth(FrameBufferObject out) {
        blit(GL_DEPTH_BUFFER_BIT, out.fbo, out.getBufferWidth(), out.getBufferHeight());
    }

    public void blit(int bit, int output, int outWidth, int outHeight) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, output);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.fbo);
        glBlitFramebuffer(0, 0, width, height, 0, 0, outWidth, outHeight, bit, GL_NEAREST);
        this.unbind();
    }

    public final int getTextureID(int attachment) {
        return colorTextures.get(attachment);
    }

    public final int getDepthTexture() {
        if (hasDepthAttachment && hasDepthTexture) {
            return depthPointer;
        } else {
            System.err.println("YOU DONT HAVE A DEPTH TEXTURE OR DEPTH BUFFER ATTACHMENT TO THAT FBO");
            return 0;
        }
    }

    public void cleanUp() {// call when closing the game
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);
        colorTextures.forEach(GL11::glDeleteTextures);
        if (hasDepthAttachment) {
            if (hasDepthTexture) {
                glDeleteTextures(depthPointer);
            } else {
                glDeleteRenderbuffers(depthPointer);
            }
        }
    }

    public FrameBufferObject bind() {
        bindFrameBuffer(fbo, width, height);
        return this;
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void clear(float r, float g, float b, float a) {
        glClearColor(r, g, b, a);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public FrameBufferObject unbind() {// call to switch to default frame buffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return this;
    }

    private void bindFrameBuffer(int frameBuffer, int width, int height) {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glViewport(0, 0, width, height);
    }

    private int createFrameBuffer(int attachmentCount) {
        int frameBuffer = glGenFramebuffers();
        // generate name for frame buffer
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        // create the framebuffer
        if(attachmentCount!=0) {
            int[] attachments = new int[attachmentCount];
            for (int i = 0; i < attachmentCount; i++) {
                attachments[i] = GL_COLOR_ATTACHMENT0 + i;
            }
            glDrawBuffers(attachments);
        }else {
            glDrawBuffers(GL_NONE);
        }
        return frameBuffer;
    }

    private int createTextureAttachment(int type, int precision, int format, int attachment) {
        int colourTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colourTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, type, width, height, 0, format, precision, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, colourTexture, 0);
        return colourTexture;
    }

    private int createDepthBufferAttachment() {
        int depthBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);
        return depthBuffer;
    }

    private int createDepthTextureAttachment(boolean mipmapped) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        if (mipmapped) {
            isMipMappedDepth = true;
            glGenerateMipmap(GL_TEXTURE_2D);
        }
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        GL32.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, texture, 0);
        return texture;
    }

    public static void cleanUpAllFbos() {
        allFbos.forEach(FrameBufferObject::cleanUp);
    }

    public int getBufferHeight() {
        return height;
    }

    public int getBufferWidth() {
        return width;
    }


    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        for (int texture : colorTextures) {
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }
        if (hasDepthTexture) {
            if (isMipMappedDepth) {
                glDeleteTextures(depthPointer);
                depthPointer = createDepthTextureAttachment(isMipMappedDepth);
            } else {
                glBindTexture(GL_TEXTURE_2D, getDepthTexture());
                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
            }
        }
        if (hasDepthAttachment && !hasDepthTexture) {
            System.err.println("renderbuffer resizing not supported");
        }
    }
}