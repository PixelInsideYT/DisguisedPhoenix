package graphics.objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static org.lwjgl.opengl.GL11.*;

public class OpenGLState {

    private static boolean alphaBlending = false;

    public static void enableAlphaBlending() {
        if (!alphaBlending) {
            GL11.glEnable(GL12.GL_BLEND);
            alphaBlending = true;
        }
    }

    public static void disableAlphaBlending() {
        if (alphaBlending) {
            GL11.glDisable(GL12.GL_BLEND);
            alphaBlending = false;
        }
    }

    private static int blendingMode = 0;

    public static void setAlphaBlending(int mode) {
        enableAlphaBlending();
        if (mode != blendingMode) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, mode);
            blendingMode = mode;
        }
    }

    private static boolean depthTest = false;

    public static void enableDepthTest() {
        if (!depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            depthTest = true;
        }
    }

    public static void disableDepthTest() {
        if (depthTest) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            depthTest = false;
        }
    }

    private static boolean backFaceCulling = false;

    public static void enableBackFaceCulling() {
        if (!backFaceCulling) {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL12.GL_BACK);
            backFaceCulling = true;
        }
    }

    public static void disableBackFaceCulling() {
        if (backFaceCulling) {
            GL11.glDisable(GL11.GL_CULL_FACE);
            backFaceCulling = false;
        }
    }

    private static boolean wireframe = false;

    public static void enableWireframe() {
        if (!wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            wireframe = true;
        }
    }

    public static void disableWireframe() {
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            wireframe = false;
        }
    }

}
