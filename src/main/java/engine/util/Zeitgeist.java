package engine.util;


public class Zeitgeist {

    public static float FPS_CAP = 900;
    long lastFrame = System.currentTimeMillis();
    float delta;

    private int fpsCounter = 0;
    private float millisCounter = 0;
    private int currentFPS;

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sleep() {
        updateTimeDelta();
        sleep(getDelay());
    }

    public void updateTimeDelta() {
        delta = (System.currentTimeMillis() - lastFrame) / 1000f;
        millisCounter += delta;
        if (millisCounter >= 1) {
            currentFPS = fpsCounter;
            fpsCounter = 0;
            millisCounter = 0;
        }
        fpsCounter++;
        lastFrame = System.currentTimeMillis();
    }

    public float getDelta() {
        return delta;
    }

    public int getDelay() {
        return (int) Math.max((1f / FPS_CAP - delta) * 1000, 1);
    }

    public int getFPS() {
        return currentFPS;
    }

}
