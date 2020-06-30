package engine.input;

import engine.util.BiMap;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class JoystickInputMap extends InputMap {

    protected BiMap<String, Integer> actionToAxisMap = new BiMap<>();
    protected BiMap<String, Integer> actionToKeyMap = new BiMap<>();

    public JoystickInputMap(int device) {
        deviceName = GLFW.glfwGetJoystickName(device);
    }

    public Set<Integer> getAllMapedKeys() {
        return actionToKeyMap.getValueSet();
    }

    public String getActionForKey(int key) {
        return actionToKeyMap.getKey(key);
    }

    public Set<Integer> getAllMapedAxis() {
        return actionToAxisMap.getValueSet();
    }

    public String getActionForAxis(int key) {
        return actionToAxisMap.getKey(key);
    }

    public JoystickInputMap addKeyMapping(String action, int key) {
        actionToKeyMap.put(action, key);
        return this;
    }

    public JoystickInputMap addAxisMapping(String action, int axis) {
        actionToAxisMap.put(action, axis);
        return this;
    }

}
