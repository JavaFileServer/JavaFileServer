package it.sssupclient.app;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Helpers {
    public static void sendFull(FileChannel file, SocketChannel sc, long length) throws IOException {
        long sent = 0;
        do {
            sent += file.transferTo(sent, length-sent, sc);
        } while (sent != length);
    }

    public static void panic(String error) {
        System.err.println("Panic: " + error);
        System.exit(1);
    }

    public static FileChannel readAllStdin() throws IOException {
        var cwd = Paths.get("").toAbsolutePath();
        var r = Channels.newChannel(System.in);
        var input_file = File.createTempFile("input", null, cwd.toFile());
        input_file.deleteOnExit();
        var data = FileChannel.open(input_file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.READ);
        try (var wrapper = BufferManager.getBuffer()) {
            var buffer = wrapper.get();
            while (r.read(buffer) > 0)
            {
                buffer.flip();
                data.write(buffer);
                buffer.clear();
            }
        }
        r.close();
        data.position(0L);
        return data;
    }

    static void writeAll(SocketChannel sc, ByteBuffer[] buffers) {
        try {
            boolean available;
            do {
                available = false;
                sc.write(buffers);
                for (var b : buffers) {
                    if (b.hasRemaining()) {
                        available = true;
                        break;
                    }
                }
            } while (available);
        } catch (Exception e) {
            panic("Failed writing to SocketChannel: " + e);
        }
    }

    public static void writeAll(SocketChannel sc, ByteBuffer buffer) {
        writeAll(sc, new ByteBuffer[]{buffer});
    }

    public static BufferManager.BufferWrapper readBytes(SocketChannel sc, int length) {
        var wrapper = BufferManager.getBuffer();
        var buf = wrapper.get();
        buf.limit(length);
        do {
            try {
                sc.read(buf);
            } catch (Exception e) {
                System.err.println("Read failed: " + e);
            }
        } while(buf.hasRemaining());
        buf.flip();
        return wrapper;
    }

    public static long readLong(SocketChannel sc) {
        try (var ans = readBytes(sc, 8);) {
            return ans.get().getLong();
        }
    }

    public static int readInt(SocketChannel sc) {
        try (var ans = readBytes(sc, 4);) {
            return ans.get().getInt();
        }
    }

    public static short readShort(SocketChannel sc) {
        try (var ans = readBytes(sc, 2);) {
            return ans.get().getShort();
        }
    }

    public static short readByte(SocketChannel sc) {
        try (var ans = readBytes(sc, 1);) {
            return ans.get().get();
        }
    }

    public static String readString(SocketChannel sc) {
        var length = readInt(sc);
        if (length == 0) {
            return "";
        }
        try (var wrapper = readBytes(sc, length);)
        {
            var data = wrapper.get();
            var bytes = new byte[data.remaining()];
            data.get(bytes);
            var string = new String(bytes, StandardCharsets.UTF_8);
            return string;
        }
    }

    public static byte[] serializeString(String string) {
        if (string == null || string.isEmpty()) {
            return new byte[4];
        }
        var data = string.getBytes(StandardCharsets.UTF_8);
        var bytes = new ByteArrayOutputStream(4 + data.length);
        var bs = new DataOutputStream(bytes);
        try {
            bs.writeInt(data.length);
            bs.write(data);
            bs.flush();
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            System.exit(1);
        }
        return bytes.toByteArray();
    }

    static void checkInt(SocketChannel sc, int value) {
        var ans = readInt(sc);
        if (ans != value) {
            panic("Bad read: found " + ans + " instead of " + value);
        }
    }

    static void checkShort(SocketChannel sc, short value) {
        var ans = readShort(sc);
        if (ans != value) {
            panic("Bad read: found " + ans + " instead of " + value);
        }
    }

    static void checkVersion(SocketChannel sc, int version) {
        checkInt(sc, version);
    }

    static void checkType(SocketChannel sc, short type) {
        checkShort(sc, type);
    }

    static void checkAns(SocketChannel sc) {
        checkShort(sc, (short)1);
    }

    public static void checkPadding(SocketChannel sc, int length) {
        var padding = new byte[length];
        try (var buffer = readBytes(sc, length);) {
            buffer.get().get(padding);
            for (var b : padding) {
                if (b != 0) {
                    panic("padding: found " + b);
                }
            }
        }
    }
}
