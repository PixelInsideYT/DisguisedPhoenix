package engine.util;

import java.util.HashMap;
import java.util.Set;

public class BiMap<K, V> {

    private final HashMap<K, V> map = new HashMap<>();
    private final HashMap<V, K> inversedMap = new HashMap<>();

    public void put(K k, V v) {
        map.put(k, v);
        inversedMap.put(v, k);
    }

    public V get(K k) {
        return map.get(k);
    }

    public K getKey(V v) {
        return inversedMap.get(v);
    }

    public Set<K> getSet() {
        return map.keySet();
    }

    public Set<V> getValueSet() {
        return inversedMap.keySet();
    }

    public void removeKey(K k) {
        V v = map.get(k);
        map.remove(k);
        inversedMap.remove(v);
    }

    public void removeValue(V v) {
        K k = inversedMap.get(v);
        inversedMap.remove(v);
        map.remove(k);
    }


}
