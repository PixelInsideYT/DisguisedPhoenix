package engine.input;

public class MouseInputMap extends InputMap {

    public static final String CURRENT_MOUSE_POS_X = "mousePosX";
    public static final String CURRENT_MOUSE_POS_Y = "mousePosY";
    public static final String MOUSE_DX = "dx";
    public static final String MOUSE_DY = "dy";
    public static final String BUTTON_LEFT = "left";
    public static final String BUTTON_MIDDLE = "middle";
    public static final String BUTTON_RIGHT = "right";

    public MouseInputMap() {
        this.actionToValueMap.put(CURRENT_MOUSE_POS_X, 0f);
        this.actionToValueMap.put(CURRENT_MOUSE_POS_Y, 0f);
        this.actionToValueMap.put(MOUSE_DX, 0f);
        this.actionToValueMap.put(MOUSE_DY, 0f);
        this.actionToValueMap.put(BUTTON_LEFT, 0f);
        this.actionToValueMap.put(BUTTON_MIDDLE, 0f);
        this.actionToValueMap.put(BUTTON_RIGHT, 0f);
    }

}
