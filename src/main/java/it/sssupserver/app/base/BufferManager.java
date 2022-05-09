package it.sssupserver.app.base;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BufferManager
 */
public class BufferManager {
    // no more than MAX_BUFFER_COUNT buffers will be allocated
    private static AtomicInteger buffer_count = new AtomicInteger();
    public static final int MAX_BUFFER_COUNT = 1 << 9;
    public static final int DEFAULT_BUFFER_SIZE = 1 << 24;

    private static BlockingQueue<ByteBuffer> bufferQueue = new LinkedBlockingQueue<>(MAX_BUFFER_COUNT);

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

    // Return a buffer in an AutoCloseable container
    // that will automatically reinsert the buffer into
    // the pool when close();
    public static BufferWrapper getBuffer() throws InterruptedException {
        var buffer = bufferQueue.poll();
        if (buffer != null) {
            return new BufferWrapper(buffer);
        } else {
            try {
                var now = buffer_count.incrementAndGet();
                if (now <= MAX_BUFFER_COUNT) {
                    return new BufferWrapper(ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE));
                } else {
                    return new BufferWrapper(bufferQueue.take());
                }
            } catch (OutOfMemoryError e) {
                buffer_count.decrementAndGet();
                return new BufferWrapper(bufferQueue.take());
            }
        }
    }
}
