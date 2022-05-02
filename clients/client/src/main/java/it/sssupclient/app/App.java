package it.sssupclient.app;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.Thread.State;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    static void writeAll(SocketChannel sc, ByteBuffer buffer) {
        writeAll(sc, new ByteBuffer[]{buffer});
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
        var bytes = new byte[data.remaining()];
        data.get(bytes);
        var string = new String(bytes, StandardCharsets.UTF_8);
        putBuffer(data);
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
    static ByteBuffer buildRead(String username, String path, long offset, long length) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)1);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        buffer.putInt((int)offset);
        buffer.putInt((int)length);
        buffer.flip();
        return buffer;
    }

    @FunctionalInterface
    interface ReadConsumer {
        void consume(long offset, boolean last, ByteBuffer data);
    }
//    •	Numero di versione a 1, 4 byte:
//    •	Codice comando a 1, 2 byte
//    •	Categoria messaggio a 1 (risposta), 2 byte
//    •	Status, 2 byte: la richiesta era ben formata?
//    o	Status = 0, lettura
//    	2 byte bitfield, campi di default a 0:
//    •	Bit 0: partial chunk, se non 0 la risposta arriverà frammentata e questo non è l’ultimo blocco
//    	4 byte: offset dall’inizio del file
//    	4 byte: lunghezza del chunk
//    	Sequenza di byte lunga quanto indicato nel campo precedente
//    o	Status = 1, errore
//    	2 byte a 0: padding
    static final int MAX_CHUNK_SIZE = 1 << 16;

    static class Pair<T1,T2> {
        public T1 first;
        public T2 second;
    }

    /**
     * ans.first    = success (not error)
     * ans.second   = (if first == true) last chunk
     */
    static Pair<Boolean,Boolean> parseRead(SocketChannel sc, int version, ReadConsumer consumer) {
        Pair<Boolean,Boolean> ans = new Pair<Boolean,Boolean>();
        int tmp = 0;
        checkVersion(sc, version);
        checkType(sc, (short)1);
        checkAns(sc);
        var status = readShort(sc);
        switch (status) {
        case 0:
            var flags = readShort(sc);
            var last = (flags & 1) == 0;
            int offset = readInt(sc);
            int toRead = readInt(sc);
            var buffer = getBuffer();
            var read = 0;
            do {
                // read
                var blckSize = Math.min(toRead-read, buffer.remaining());
                buffer.limit(blckSize);
                try {
                    tmp = sc.read(buffer);
                } catch (Exception e) {
                    panic("error" + e);
                }
                if (tmp < 0) {
                    System.err.println("Error while reading data");
                }
                toRead += tmp;
                // consume
                buffer.flip();
                consumer.consume(offset + read, last && toRead == read, buffer);
                buffer.clear();
            } while (toRead > read);
            putBuffer(buffer);
            ans.first = true;
            ans.second = last;
        case 1:
            ans.first = false;
            break;
        default:
            panic("Invalid status: " + status);
        }
        return ans;
    }

    static ByteBuffer[] buildCreateOrReplace(String username, String path, ByteBuffer[] content) {
        var ans = new ByteBuffer[1+content.length];
        var header = getBuffer();
        var version = username == null ? 1 : 2;
        header.putInt(version);
        if (version == 2) {
            header.put(serializeString(username));
        }
        header.putShort((short)2);
        header.putShort((short)0);
        header.put(serializeString(path));
        int length = 0;
        for (var c : content) {
            length += c.remaining();
        }
        header.putInt(length);
        header.flip();
        ans[0] = header;
        for (int i=0; i!=content.length; ++i) {
            ans[i+1] = content[i];
        }
        return ans;
    }

    static boolean parseCreateOrReplaceAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)2);
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
        buffer.flip();
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
        buffer.flip();
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

    static ByteBuffer buildListReq(String username, String path) {
        var buffer = getBuffer();
        var version = username == null ? 1 : 2;
        buffer.putInt(version);
        if (version == 2) {
            buffer.put(serializeString(username));
        }
        buffer.putShort((short)7);
        buffer.putShort((short)0);
        buffer.put(serializeString(path));
        buffer.flip();
        return buffer;
    }

    static String[] parseListAns(SocketChannel sc, int version) {
        checkVersion(sc, version);
        checkType(sc, (short)7);
        checkAns(sc);
        var status = readShort(sc);
        switch (status) {
        case 0:
            checkPadding(sc, 2);
            var length = readInt(sc);
            var ans = new String[length];
            for (int i=0; i!=length; ++i) {
                ans[i] = readString(sc);
            }
            Arrays.sort(ans);
            return ans;
        case 1:
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
        buffer.flip();
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
        buffer.flip();
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
        buffer.flip();
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

    static String host = "localhost";
    static int port = 5050;
    static SocketChannel connect() {
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
        public final String description;
        public final Consumer<String[]> handler;
        public Handler(String usage, Consumer<String[]> handler, String description) {
            this.usage = usage;
            this.handler = handler;
            this.description = description;
        }
    }

    static Map<String, Handler> handlers = new TreeMap<>();

    static void addListhandler() {
        handlers.put("list",
        new Handler("[path]",
        (String[] args) -> {
            String username = null;
            var path = args.length > 0 ? args[0] : ".";
            var sc = connect();
            var r = buildListReq(username, path);
            writeAll(sc, r);
            putBuffer(r);
            var files = parseListAns(sc, username != null ? 2 : 1);
            if (files != null) {
                System.out.println("Found " + files.length + " files:");
                for (int i=0; i != files.length; ++i) {
                    System.out.println(i + ") " + files[i]);
                }
            } else {
                System.err.println("Bad request: bad path?");;
            }
        }, "list files at the given location or root by default"));
    }

    static void addHandlers() {
        addListhandler();
    }

    static void help() {
        System.err.println("Usage:");
        System.err.println("\tcommand [args]");
        System.err.println();
        System.err.println("\tAvailable commands:");
        for (var cmd : handlers.entrySet()) {
            System.err.println("\t\t" + cmd.getKey() + " " + cmd.getValue().usage);
            System.err.println("\t\t\t" + cmd.getValue().description);
            System.err.println();
        }
        System.exit(1);
    }

    static void handleCmd(String cmd, String[] args) {
        var h = handlers.get(cmd);
        if (h == null) {
            System.err.println("Unknown command: " + cmd);
            help();
        }
        h.handler.accept(args);
    }

    static void handle(String[] args) {
        var cmd = args[0];
        args = getArgs(args, 1);
        handleCmd(cmd, args);
    }

    public static void main( String[] args ) throws Exception
    {
        args = new String[]{
            "list"
        };

        addHandlers();
        if (args.length == 0 || args[0].equals("--help")) {
            help();
        }
        handle(args);

    }
}
