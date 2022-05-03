package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle command handler.
 * When a command is executed its handler
 * is passed to the scheduler in order
 */
public class Scheduler {
    private SocketChannel sc;
    // version | marker | command
    private Map<Integer, Map<Integer, Map<Short, Command>>> map = new HashMap<>();

    public Scheduler(SocketChannel sc) {
        this.sc = sc;
    }

    public void schedule(Waiter handler) {
        this.map.compute(handler.getVersion(), (k, v) -> {
            if (v == null) {
                v = new HashMap<>();
            }
            v.compute(handler.getMarker(), (marker, m) -> {
                if (m == null) {
                    m = new HashMap<>();
                }
                m.put(handler.getType(), handler.getHandler());
                return m;
            });
            return v;
        });
    }

    public void parse(SocketChannel sc) {
        ...
    }
}
