package it.sssupclient.app;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * BufferManager
 */
public class BufferManager {
    public static final int DEFAULT_BUFFER_SIZE = 1 << 16;

    private static Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();

    public static class BufferWrapper implements AutoCloseable {
        private ByteBuffer buffer;
        private BufferWrapper(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        public ByteBuffer get() {
            return this.buffer;
        }

        @Override
        public void close() {
            buffer.clear();
            bufferQueue.add(buffer);
        }
    }

    public static BufferWrapper getBuffer() {
        var buffer = bufferQueue.poll();
        return new BufferWrapper(buffer == null
                ? ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                : buffer);
    }
}
