package it.sssupserver.app.handlers.simplebinaryhandler;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import it.sssupserver.app.base.BufferManager;

/**
 * Auxiliary class with static helper methods
 */
public class SimpleBinaryHelper {
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
}
