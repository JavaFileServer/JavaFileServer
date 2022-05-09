package it.sssupserver.app.base;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * BufferManager
 */
public class BufferManager {
    public static final int DEFAULT_BUFFER_SIZE = 1 << 24;

    private static Queue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();

    public static class BufferWrapper implements AutoCloseable {
        private ByteBuffer buffer;
        private BufferWrapper(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        public ByteBuffer get() {
            return this.buffer;
        }

        private boolean closed;
        @Override
        public void close() {
            if (!closed) {
                buffer.clear();
                bufferQueue.add(buffer);
                closed = true;
            }
        }

        public boolean isClosed() {
            return closed;
        }
    }

    public static BufferWrapper getBuffer() {
        var buffer = bufferQueue.poll();
        try {
            return new BufferWrapper(buffer == null
                    ? ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                    : buffer);            
        } catch (Exception e) {
            System.err.println("Catastrofe: " + e);
            return null;
        }
    }
}
