package de.thriemer.engine.input;

import java.util.HashMap;
import java.util.Map;

public abstract class InputMap {

    protected Map<String, Float> actionToValueMap = new HashMap<>();
    String deviceName;
    int deviceNumber;

    public float getValueForAction(String action) {
        Float rt = actionToValueMap.get(action);
        return rt == null ? 0f : rt;
    }

    public void setValue(String action, float value) {
        actionToValueMap.put(action, value);
    }
}