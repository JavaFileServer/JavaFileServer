package it.sssupclient.app;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Hello world!
 *
 */
public class App 
{
    static void panic(String error) {
        System.err.println("Panic: " + error);
        System.exit(1);
    }

    static final int BUF_SIZE = 1 << 16;
    static Queue<ByteBuffer> bufQueue = new LinkedList<ByteBuffer>();

    static ByteBuffer getBuffer() {
        var ans = bufQueue.poll();
        return ans == null
            ? ByteBuffer.allocateDirect(BUF_SIZE)
            : ans;
    }

    static void putBuffer(ByteBuffer buffer) {
        buffer.clear();
        bufQueue.add(buffer);
    }

    static ByteBuffer readBytes(SocketChannel sc, int length) {
        var buf = getBuffer();
        buf.limit(length);
        do {
            try {
                sc.read(buf);
            } catch (Exception e) {
                System.err.println("Read failed: " + e);
            }
        } while(buf.hasRemaining());
        buf.flip();
        return buf;
    }

    static int readInt(SocketChannel sc) {
        var ans = readBytes(sc, 4);
        return ans.getInt();
    }

    static short readShort(SocketChannel sc) {
        var ans = readBytes(sc, 2);
        return ans.getShort();
    }

    static short readByte(SocketChannel sc) {
        var ans = readBytes(sc, 1);
        return ans.get();
    }

    static String readString(SocketChannel sc) {
        var length = readInt(sc);
        var data = readBytes(sc, length);
        var string = data.asCharBuffer().toString();
        return string;
    }

    public static byte[] serializeString(String string) {
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

    static void checkPadding(SocketChannel sc, int length) {
        var padding = new byte[length];
        var buffer = readBytes(sc, length);
        buffer.get(padding);
        for (var b : padding) {
            if (b != 0) {
                panic("padding: found " + b);
            }
        }
        putBuffer(buffer);
    }

    static ByteBuffer listExists(String username, String path) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)3);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        return buffer;
    }

    static boolean parseExistsAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)3);
        checkAns(sc);
        var status = readByte(sc);
        checkPadding(sc, 3);
        boolean ans = false;
        switch (status) {
        case 0:
            ans = false;
        case 1:
            ans = false;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }

    static ByteBuffer listTruncate(String username, String path) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)4);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        return buffer;
    }

    static boolean parseTruncateAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)4);
        checkAns(sc);
        var status = readByte(sc);
        checkPadding(sc, 3);
        boolean ans = false;
        switch (status) {
        case 0:
            ans = false;
        case 1:
            ans = false;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }

    static ByteBuffer listDelete(String username, String path) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)6);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        return buffer;
    }

    static boolean parseDeleteAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)6);
        checkAns(sc);
        var status = readByte(sc);
        checkPadding(sc, 3);
        boolean ans = false;
        switch (status) {
        case 0:
            ans = false;
        case 1:
            ans = false;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }

    static ByteBuffer listMsg(String username, String path) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)7);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        return buffer;
    }

    static String[] parseListAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)7);
        checkAns(sc);
        var status = readShort(sc);
        switch (status) {
        case 1:
            checkPadding(sc, 2);
            var length = readInt(sc);
            var ans = new String[length];
            for (int i=0; i!=length; ++i) {
                ans[i] = readString(sc);
            }
            return ans;
        case 2:
            return null;
        default:
            panic("Invalid status: " + status);
        }
        return null;
    }

    static ByteBuffer listCopy(String username, String source, String destination) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)10);
        buffer.putShort((short)0);
        buffer.put(serializeString(source));
        buffer.put(serializeString(destination));
        return buffer;
    }

    static boolean parseCopyAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)10);
        checkAns(sc);
        var status = readByte(sc);
        checkPadding(sc, 3);
        boolean ans = false;
        switch (status) {
        case 0:
            ans = false;
        case 1:
            ans = false;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }


    static ByteBuffer listMove(String username, String source, String destination) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)11);
        buffer.putShort((short)0);
        buffer.put(serializeString(source));
        buffer.put(serializeString(destination));
        return buffer;
    }

    static boolean parseMoveAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)11);
        checkAns(sc);
        var status = readByte(sc);
        checkPadding(sc, 3);
        boolean ans = false;
        switch (status) {
        case 0:
            ans = false;
        case 1:
            ans = false;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }

    static ByteBuffer listMkdir(String username, String path) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)12);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        return buffer;
    }

    static boolean parseMkdirAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)12);
        checkAns(sc);
        var status = readByte(sc);
        checkPadding(sc, 3);
        boolean ans = false;
        switch (status) {
        case 0:
            ans = false;
        case 1:
            ans = false;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }

    static SocketChannel connect(String host, int port) {
        try {
            var sc = SocketChannel.open(new InetSocketAddress(host, port));
            return sc;
        } catch (Exception e) {
            System.err.println("Cannot connect: " + e);
            System.exit(1);
        }
        return null; // never reached
    }

    /**
     * Extract trailing arguments
     */
    static String[] getArgs(String[] args, int skip) {
        if (args.length < skip) {
            System.err.println("Missing arguments");
            System.exit(1);
        }
        var ans = new String[args.length - skip];
        for (int i=0; i != ans.length; ++i) {
            ans[i] = args[skip+i];
        }
        return ans;
    }

    static class Handler {
        public final String usage;
        public final Consumer<String[]> handler;
        public Handler(String usage, Consumer<String[]> handler) {
            this.usage = usage;
            this.handler = handler;
        }
    }

    static Map<String, Handler> handlers = new TreeMap<>();

    static void addListhandler() {
        handlers.put("list", new Handler("[path]",
        (String[] args) -> {

        }));
    }

    static void addHandlers() {
        addListhandler();
    }

    static void help() {
        System.err.println("Usage:");
        System.err.println("\tcommand [args]");
        System.exit(1);
    }

    public static void main( String[] args ) throws Exception
    {
        addHandlers();
        if (args.length == 0) {
            help();
        }
        for (int i=0; i < args.length; ++i) {
            System.out.println(i + ") " + args[i]);
        }
        System.out.println( "Hello World!" );
    }
}
