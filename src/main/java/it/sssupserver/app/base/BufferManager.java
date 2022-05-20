package it.sssupserver.app.base;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BufferManager
 */
public class BufferManager {
    // no more than MAX_BUFFER_COUNT buffers will be allocated
    private static AtomicInteger buffer_count = new AtomicInteger();
    public static final int MIN_BUFFER_SIZE = 1 << 10;
    // default number of buffers allocated on demand
    public static final int DEFAULT_BUFFER_COUNT = 1 << 9;
    // default size of an allocated buffer
    public static final int DEFAULT_BUFFER_SIZE = 1 << 24;
    // default upper limit for the memory used by all buffers
    public static final long DEFAULT_MEMORY_LIMIT = (long)DEFAULT_BUFFER_SIZE*(long)DEFAULT_BUFFER_COUNT;
    private static int buffer_size = DEFAULT_BUFFER_SIZE;
    private static int max_buffer_count = DEFAULT_BUFFER_COUNT;

    // use direct buffers
    private static boolean default_direct = true;
    private static boolean use_direct = true;
    
    // Command line arguments starting with this prefix
    // are intended as directed to the buffer manager
    private static final String argsPrefix = "--M";

    public static void Help(String lpadding) {
        if (lpadding == null) {
            lpadding = "";
        }
        System.err.println(lpadding + "Arguments recognised by request handlers:");
        System.err.println(lpadding + "\t" + argsPrefix + "bufsz size: buffer size in KB");
        System.err.println(lpadding + "\t" + argsPrefix + "limit size: max size in KB for all buffers");
        System.err.println(lpadding + "\t" + argsPrefix + "direct: use direct buffers" + (BufferManager.default_direct ? " (default)" : ""));
        System.err.println(lpadding + "\t" + argsPrefix + "nodirect: do not use direct buffers" + (!BufferManager.default_direct ? " (default)" : ""));
    }

    public static void parseArgs(String[] args) {
        try {
            if (args != null) {
                int bufsz = 0;
                long limit = 0L;
                boolean direct = false, nodirect = false;
                for (int i=0; i!=args.length; ++i) {
                    var a = args[i];
                    if (a.equals("--")) {
                        break;
                    } else if (a.startsWith(argsPrefix)) {
                        var parameter = a.substring(argsPrefix.length());
                        switch (parameter) {
                            case "bufsz":
                                if (i + 1 == args.length) {
                                    throw new Exception("Missing value for parameter bufsz");
                                }
                                bufsz = Integer.parseInt(args[++i]) << 10;
                                break;
                            case "limit":
                                if (i + 1 == args.length) {
                                    throw new Exception("Missing value for parameter limit");
                                }
                                limit = Long.parseLong(args[++i]) << 10;
                                break;
                            case "direct":
                                direct = true;
                                break;
                            case "nodirect":
                                nodirect = true;
                                break;
                            default:
                                throw new Exception("Unrecognised argument '" + a + "'");
                        }
                    }
                }
                if (nodirect && direct) {
                    throw new Exception("Cannot mix '" + argsPrefix + "direct' with '" + argsPrefix + "nodirect'");
                } else {
                    BufferManager.use_direct = nodirect ? false : (direct ? true :  BufferManager.default_direct);
                }
                if (bufsz == 0) {
                    bufsz = DEFAULT_BUFFER_SIZE;
                }
                if (bufsz < MIN_BUFFER_SIZE) {
                    throw new Exception("bufsz (" + bufsz + ") cannot be lower than " + MIN_BUFFER_SIZE + " (in bytes)");
                }
                if (limit == 0) {
                    limit = DEFAULT_MEMORY_LIMIT;
                }
                if (limit < bufsz) {
                    throw new Exception("limit (" + limit + ") cannot be lower than bufsz (" + bufsz + ")");
                }
                // check args coerence
                if (limit % bufsz != 0) {
                    throw new Exception("limit (" + limit + ") is not a multiple of bufsz (" + bufsz + ")");
                }
                BufferManager.buffer_size = bufsz;
                BufferManager.max_buffer_count = (int)(limit / bufsz);
                // check for overflow
                if ((long)max_buffer_count != limit / bufsz) {
                    throw new Exception("Cannot handle " + limit / bufsz + " buffers");
                }
                // should queue be resized?
                if (DEFAULT_BUFFER_COUNT != max_buffer_count) {
                    bufferQueue = new ArrayBlockingQueue<>(max_buffer_count);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            Help("\t");
            System.exit(1);
        }
    }

    private static BlockingQueue<ByteBuffer> bufferQueue = new ArrayBlockingQueue<>(DEFAULT_BUFFER_COUNT);

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
                if (now <= max_buffer_count) {
                    return new BufferWrapper(BufferManager.use_direct ? ByteBuffer.allocateDirect(buffer_size) : ByteBuffer.allocate(buffer_size));
                } else {
                    buffer_count.decrementAndGet();
                    return new BufferWrapper(bufferQueue.take());
                }
            } catch (OutOfMemoryError e) {
                buffer_count.decrementAndGet();
                return new BufferWrapper(bufferQueue.take());
            }
        }
    }
}
