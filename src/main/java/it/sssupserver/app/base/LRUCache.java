package it.sssupserver.app.base;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintain an LRU cache of fixed size.
 */
public class LRUCache<K,V> {
    private Map<K,V> map = new ConcurrentHashMap<>();
    public int default_limit = 100;
    private int limit = default_limit;
    private LinkedList<K> list = new LinkedList<>();

    public LRUCache(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
    }

    public synchronized V get(K key) {
        var v = map.get(key);
        if (v == null) {
            return null;
        }
        list.removeLastOccurrence(key);
        list.addFirst(key);
        return v;
    }

    /**
     * Insert a new value (eventually returning
     * the old correspondent value)
     */
    public synchronized V put(K key, V val) {
        var v = map.replace(key, val);
        list.addFirst(key);
        if (v != null) {
            list.removeLastOccurrence(key);
            return v;
        }
        if (map.size() > this.limit) {
            map.remove(list.removeLast());
        }
        return null;
    }

    public synchronized V remove(K key) {
        var v = map.remove(key);
        if (v != null) {
            list.remove(key);
        }
        return v;
    }

    public synchronized void clear() {
        map.clear();
        list.clear();
    }
}
