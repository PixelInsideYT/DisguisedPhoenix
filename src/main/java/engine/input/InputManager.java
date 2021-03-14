package engine.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWJoystickCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InputManager {

    private static final int KEYBOARD_SIZE = 512;
    protected GLFWJoystickCallback joystickListner = new GLFWJoystickCallback() {

        @Override
        public void invoke(int jid, int event) {
            System.out.println(jid + "/" + GLFW.glfwGetJoystickName(jid) + " "
                    + (event == GLFW.GLFW_CONNECTED ? "connected" : "disconnected"));
        }
    };
    private final long window;
    private final int[] keyStates = new int[KEYBOARD_SIZE];
    private final boolean[] activeKeys = new boolean[KEYBOARD_SIZE];
    private final List<InputMap> deviceInputs = new ArrayList<>();
    private final List<GLFWKeyCallback> keyCallbacks = new ArrayList<>();
    protected GLFWKeyCallback keyboard = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key != -1) {
                activeKeys[key] = action != GLFW.GLFW_RELEASE;
                keyStates[key] = action;
            }
            keyCallbacks.forEach(c -> c.invoke(window, key, scancode, action, mods));
        }
    };
    private float currentMouseX;
    private float currentMouseY;
    private float lastMouseX;
    private float lastMouseY;
    protected GLFWCursorPosCallback mousePosListener = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double xpos, double ypos) {
            currentMouseX = (float) xpos;
            currentMouseY = (float) ypos;
        }
    };
    private int leftMouse;
    private int middleMouse;
        private int rightMouse;

    public InputManager(long window) {
        this.window = window;
        GLFW.glfwSetKeyCallback(window, keyboard);
        GLFW.glfwSetCursorPosCallback(window, mousePosListener);
        GLFW.glfwSetJoystickCallback(joystickListner);

    }

    public void addKeyCallback(GLFWKeyCallback callback) {
        keyCallbacks.add(callback);
    }

    public void addInputMapping(InputMap map) {
        deviceInputs.add(map);
    }

    public boolean isKeyDown(int key) {
        return activeKeys[key];
    }

    public int[] getActiveKeys() {
        List<Integer> pressedKeys = new ArrayList<>();
        for (int i = 0; i < activeKeys.length; i++) {
            if (activeKeys[i]) {
                pressedKeys.add(i);
            }
        }
        return pressedKeys.stream().mapToInt(i -> i).toArray();
    }

    public void updateInputMaps() {
        leftMouse = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        middleMouse = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        rightMouse = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        for (InputMap im : deviceInputs) {
            if (im instanceof JoystickInputMap) {
                updateJoystick((JoystickInputMap) im);
            } else if (im instanceof KeyboardInputMap) {
                updateKeyboard((KeyboardInputMap) im);
            } else if (im instanceof MouseInputMap) {
                updateMouseInputmap((MouseInputMap) im);
            }
        }
        lastMouseX = currentMouseX;
        lastMouseY = currentMouseY;
    }

    private void updateMouseInputmap(MouseInputMap im) {
        float dx = lastMouseX - currentMouseX;
        float dy = lastMouseY - currentMouseY;
        im.setValue(MouseInputMap.MOUSE_DX, dx);
        im.setValue(MouseInputMap.MOUSE_DY, dy);
        im.setValue(MouseInputMap.CURRENT_MOUSE_POS_X, currentMouseX);
        im.setValue(MouseInputMap.CURRENT_MOUSE_POS_Y, currentMouseY);
        im.setValue(MouseInputMap.BUTTON_LEFT, leftMouse == GLFW.GLFW_PRESS ? 1 : 0);
        im.setValue(MouseInputMap.BUTTON_MIDDLE, middleMouse == GLFW.GLFW_PRESS ? 1 : 0);
        im.setValue(MouseInputMap.BUTTON_RIGHT, rightMouse == GLFW.GLFW_PRESS ? 1 : 0);
    }

    private void updateKeyboard(KeyboardInputMap keyboardMap) {
        for (int key : keyboardMap.getAllMapedKeys()) {
            keyboardMap.setValue(keyboardMap.getActionForKey(key), activeKeys[key] ? 1 : 0);
        }
    }

    private void updateJoystick(JoystickInputMap controllerMap) {
        ByteBuffer buttons = GLFW.glfwGetJoystickButtons(controllerMap.deviceNumber);
        FloatBuffer axis = GLFW.glfwGetJoystickAxes(controllerMap.deviceNumber);
        byte[] buttonsArray = new byte[buttons.capacity()];
        float[] axisArray = new float[axis.capacity()];
        buttons.get(buttonsArray);
        axis.get(axisArray);
        for (int key : controllerMap.getAllMapedKeys()) {
            controllerMap.setValue(controllerMap.getActionForKey(key), buttonsArray[key]);
        }
        for (int axe : controllerMap.getAllMapedAxis()) {
            controllerMap.setValue(controllerMap.getActionForAxis(axe), axisArray[axe]);
        }

    }

    public void remove(InputMap... inputmaps) {
        deviceInputs.removeAll(Arrays.asList(inputmaps));
    }

    public void hideMouseCursor() {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    public void showMouseCursor() {
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

}