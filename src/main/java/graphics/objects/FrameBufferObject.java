package graphics.objects;

import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class FrameBufferObject {

    private static List<FrameBufferObject> allFbos = new ArrayList<>();

    private int width, height;

    private int fbo;
    private List<Integer> colorTextures = new ArrayList<>();
    private boolean hasDepthAttachment = false;
    private boolean hasDepthTexture = false;
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

    public FrameBufferObject addTextureAttachment(int type, int precision, int format, int attachment) {
        int newTexture = createTextureAttachment(type, precision, format, GL30.GL_COLOR_ATTACHMENT0 + attachment);
        colorTextures.add(newTexture);
        return this;
    }

    public FrameBufferObject addDepthBuffer() {
        this.hasDepthAttachment = true;
        this.hasDepthTexture = false;
        this.depthPointer = createDepthBufferAttachment();
        return this;
    }

    public FrameBufferObject addDepthTextureAttachment() {
        this.hasDepthAttachment = true;
        this.hasDepthTexture = true;
        this.depthPointer = createDepthTextureAttachment();
        return this;
    }

    public void blitToFbo(FrameBufferObject out) {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, out.fbo);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.fbo);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, out.getBufferWidth(), out.getBufferHeight(),
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        this.unbind();
    }

    public void blitToScreen() {
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.fbo);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        this.unbind();
    }

    public final int getTextureID(int attachment) {
        return colorTextures.get(attachment);
    }

    public final int getDepthTexture() {
        if (hasDepthAttachment && hasDepthTexture) {
            return depthPointer;
        } else {
            System.err.println("YOU DONT HAVE A DEPTH TEXTURE OR DEPTH BUFFER ATTACHET TO THAT FBO");
            return 0;
        }
    }

    public void cleanUp() {// call when closing the game
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glDeleteFramebuffers(fbo);
        colorTextures.forEach(GL11::glDeleteTextures);
        if (hasDepthAttachment) {
            if (hasDepthTexture) {
                GL11.glDeleteTextures(depthPointer);
            } else {
                GL30.glDeleteRenderbuffers(depthPointer);
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


    public void unbind() {// call to switch to default frame buffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    private void bindFrameBuffer(int frameBuffer, int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
        GL11.glViewport(0, 0, width, height);
    }

    private int createFrameBuffer(int attachmentCount) {
        int frameBuffer = GL30.glGenFramebuffers();
        // generate name for frame buffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
        // create the framebuffer
        int[] attachments = new int[attachmentCount];
        for (int i = 0; i < attachmentCount; i++) {
            attachments[i] = GL30.GL_COLOR_ATTACHMENT0 + i;
        }
        GL40.glDrawBuffers(attachments);
        return frameBuffer;
    }

    private int createTextureAttachment(int type, int precision, int format, int attachment) {
        int colourTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colourTexture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, type, width, height, 0, format, precision, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, colourTexture, 0);
        return colourTexture;
    }

    private int createDepthBufferAttachment() {
        int depthBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);
        return depthBuffer;
    }

    private int createDepthTextureAttachment() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texture, 0);
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

}