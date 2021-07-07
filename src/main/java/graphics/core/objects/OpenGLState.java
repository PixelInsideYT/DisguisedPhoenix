package graphics.core.objects;

import static org.lwjgl.opengl.GL11.*;

public class OpenGLState {

    private static boolean alphaBlending = false;
    private static int blendingMode = 0;
    private static boolean depthTest = false;
    private static boolean backFaceCulling = false;
    private static boolean wireframe = false;

    public static void enableAlphaBlending() {
        if (!alphaBlending) {
            glEnable(GL_BLEND);
            alphaBlending = true;
        }
    }

    public static void disableAlphaBlending() {
        if (alphaBlending) {
            glDisable(GL_BLEND);
            alphaBlending = false;
        }
    }

    public static boolean getAlphaBlendingState() {
        return alphaBlending;
    }

    public static void setAlphaBlending(int mode) {
        enableAlphaBlending();
        if (mode != blendingMode) {
            glBlendFunc(GL_SRC_ALPHA, mode);
            blendingMode = mode;
        }
    }

    public static void enableDepthTest() {
        if (!depthTest) {
            glEnable(GL_DEPTH_TEST);
            depthTest = true;
        }
    }

    public static void disableDepthTest() {
        if (depthTest) {
            glDisable(GL_DEPTH_TEST);
            depthTest = false;
        }
    }

    public static void enableBackFaceCulling() {
        if (!backFaceCulling) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            backFaceCulling = true;
        }
    }

    public static void disableBackFaceCulling() {
        if (backFaceCulling) {
            glDisable(GL_CULL_FACE);
            backFaceCulling = false;
        }
    }

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
