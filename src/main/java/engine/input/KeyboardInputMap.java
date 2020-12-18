package engine.input;

import engine.util.BiMap;

import java.util.Set;

public class KeyboardInputMap extends InputMap {

    protected BiMap<String, Integer> actionToKeyMap = new BiMap<>();

    public KeyboardInputMap() {
        this.deviceName = "Keyboard";
    }

    public Set<Integer> getAllMapedKeys() {
        return actionToKeyMap.getValueSet();
    }

    public Integer getKeyForAction(String action) {
        return actionToKeyMap.get(action);
    }

    public String getActionForKey(int key) {
        return actionToKeyMap.getKey(key);
    }

    public KeyboardInputMap addMapping(String action, int key) {
        actionToKeyMap.put(action, key);
        return this;
    }

    public KeyboardInputMap setName(String name) {
        this.deviceName = name;
        return this;
    }

}
