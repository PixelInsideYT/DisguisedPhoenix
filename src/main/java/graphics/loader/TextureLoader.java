package graphics.loader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class TextureLoader {

    private static Map<String, Integer> nameToIdMap = new HashMap<>();

    public static int loadTexture(String resource) {
        Integer id = nameToIdMap.get(resource);
        if (id == null) {
            return loadTextureFromDisk(resource, GL_REPEAT, GL_LINEAR);
        } else {
            return id;
        }
    }

    public static int loadTexture(String resource, int wrap, int filter) {
        Integer id = nameToIdMap.get(resource);
        if (id == null) {
            return loadTextureFromDisk(resource, wrap, filter);
        } else {
            return id;
        }
    }

    private static int loadTextureFromDisk(String resource, int wrap, int filter) {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer image;
            ByteBuffer imageBuffer = LoaderUtil.ioResourceToByteBuffer(resource, 8 * 1024);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            if (!stbi_info_from_memory(imageBuffer, w, h, comp)) {
                throw new RuntimeException("Failed to read image information: " + stbi_failure_reason());
            }
            image = stbi_load_from_memory(imageBuffer, w, h, comp, 0);
            if (image == null) {
                throw new RuntimeException("Failed to load image: " + stbi_failure_reason());
            }
            int texelWidth = w.get(0);
            int texelHeight = h.get(0);
            int format = GL_RGB;
            if (comp.get(0) == 4) {
                format = GL_RGBA;
            }
            int textureID = createTexture(texelWidth, texelHeight, image, wrap, filter, format);
            stbi_image_free(image);
            nameToIdMap.put(resource, textureID);
            return textureID;
        }
    }

    private static int createTexture(int width, int height, ByteBuffer data, int wrap, int filter, int format) {
        int textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrap);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, data);
        glBindTexture(GL_TEXTURE_2D, 0);
        return textureID;
    }

    public static void cleanUpAllTextures() {
        nameToIdMap.values().forEach(GL11::glDeleteTextures);
    }
}
