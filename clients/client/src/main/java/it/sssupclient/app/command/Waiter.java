package it.sssupclient.app.command;

/**
 * 
 */
public class Waiter {
    public Waiter(int version, short type, Command handler, int marker) {
        this.version = version;
        this.type = type;
        this.handler = handler;
        this.marker = marker;
    }

    public Waiter(int version, short type, Command handler) {
        this(version, type, handler, 0);
    }

    private short type;
    public short getType() {
        return this.type;
    }

    private int version;
    public int getVersion() {
        return this.version;
    }

    public static boolean hasMarker(int version) {
        return version > 2;
    }

    public boolean hasMarker() {
        return hasMarker(this.getVersion());
    }

    /**
     * Available only for protocol
     * version > 2
     */
    private int marker;
    public int getMarker() {
        return this.marker;
    }

    private Command handler;
    public Command getHandler() {
        return this.handler;
    }
}
