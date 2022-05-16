package it.sssupclient.app.command;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import it.sssupclient.app.Helpers;

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

    public boolean parse() throws Exception {
        boolean done;
        Command handler;
        do {
            var version = Helpers.readInt(sc);
            var marker = version < 3 ? 0 : Helpers.readInt(sc);
            var type = Helpers.readShort(sc);
            var category = Helpers.readShort(sc);
            if (category != 1) {
                Helpers.panic("Bad category, found: " + category);
            }
            handler = map.get(version).get(marker).get(type);
            done = handler.parseResponseBody(sc);
        } while (!done);
        return handler.successful();
    }
}
