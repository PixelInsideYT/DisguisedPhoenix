package de.thriemer.graphics.loader;

import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class LoaderUtil {

    private LoaderUtil() {
    }

    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) {
        ByteBuffer buffer = null;
        try (InputStream source = LoaderUtil.class.getClassLoader().getResourceAsStream(resource);
             ReadableByteChannel rbc = Channels.newChannel(source)) {
            buffer = BufferUtils.createByteBuffer(bufferSize);

            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) {
                    break;
                }
                if (buffer.remaining() == 0) {
                    buffer = resizeBuffer(buffer, buffer.capacity() * 2);
                }
            }

        } catch (Exception x) {
            x.printStackTrace();
            return null;
        }
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

}
