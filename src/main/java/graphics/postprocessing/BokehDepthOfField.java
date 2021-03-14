package graphics.postprocessing;

public class BokehDepthOfField {
/*
    private final QuadRenderer renderer;
    private final Matrix4f projMatrixInv;

    private final Shader downSampleShader;
    private final Shader vertAndDiagBlurShader;
    private final Shader bokehShader;

    private final FrameBufferObject downSampledFbo;
    private final FrameBufferObject bokehVertDiagBlurFbo;
    private final FrameBufferObject bokehFbo;

    public BokehDepthOfField(float w, float h, Matrix4f projMatrix, QuadRenderer renderer) {
        float width = w/2f;
        float height = h/2f;

        this.renderer = renderer;
        projMatrixInv = new Matrix4f(projMatrix).invert();
        downSampleShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/DoF/downSample.glsl")).combine("pos");
        downSampleShader.loadUniforms( "invViewDimensions", "inputTexture","depthTexture","projMatrixInv","focusPoint","focusRange");
        downSampleShader.bind();
        downSampleShader.load2DVector("invViewDimensions", new Vector2f(1f / width, 1f / width));
        downSampleShader.loadMatrix("projMatrixInv",projMatrixInv);
        downSampleShader.connectSampler("inputTexture", 0);
        downSampleShader.connectSampler("depthTexture", 1);

        vertAndDiagBlurShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/DoF/verticalAndDiagonalBlur.glsl")).combine("pos");
        vertAndDiagBlurShader.loadUniforms("inputImage", "invViewDimensions");
        vertAndDiagBlurShader.bind();
        vertAndDiagBlurShader.load2DVector("invViewDimensions", new Vector2f(1f / width, 1f / width));
        vertAndDiagBlurShader.connectSampler("inputImage", 0);

        bokehShader = new Shader(Shader.loadShaderCode("postProcessing/quadVS.glsl"), Shader.loadShaderCode("postProcessing/DoF/rhombiBlur.glsl")).combine("pos");
        bokehShader.loadUniforms("verticalBlurTexture", "verticalAndDiagonalBlurTexture", "invViewDimensions","power");
        bokehShader.bind();
        bokehShader.load2DVector("invViewDimensions", new Vector2f(1f / width, 1f / height));
        bokehShader.connectSampler("verticalBlurTexture", 0);
        bokehShader.connectSampler("verticalAndDiagonalBlurTexture", 1);

        downSampledFbo = new FrameBufferObject((int) width, (int) height, 1).addUnclampedTexture(0);
        bokehVertDiagBlurFbo = new FrameBufferObject((int) width, (int) height, 2).addUnclampedTexture( 0).addUnclampedTexture(1);
        bokehFbo = new FrameBufferObject((int) w, (int) h, 1).addUnclampedTexture(0);
    }
    public void render(int color, int depth) {
        boolean alphState = OpenGLState.getAlphaBlendingState();
        OpenGLState.disableAlphaBlending();
        downSampledFbo.bind();
        downSampleShader.bind();
        computeCameraParams(depth);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,color);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D,depth);
        renderer.renderOnlyQuad();
        downSampleShader.unbind();
        downSampledFbo.unbind();
        bokehVertDiagBlurFbo.bind();
        vertAndDiagBlurShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, downSampledFbo.getTextureID(0));
        renderer.renderOnlyQuad();
        vertAndDiagBlurShader.unbind();
        bokehVertDiagBlurFbo.unbind();
        bokehFbo.bind();
        bokehShader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, bokehVertDiagBlurFbo.getTextureID(0));
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, bokehVertDiagBlurFbo.getTextureID(1));
        renderer.renderOnlyQuad();
        bokehShader.unbind();
        bokehFbo.unbind();
        if(alphState)OpenGLState.enableAlphaBlending();
    }

    public int getResult() {
        return bokehFbo.getTextureID(0);
    }

    private float lastFocusPoint = 0;
    private final float lerpSpeed = 0.6f;

    private void computeCameraParams(int depth) {
        //get depth texture
        int mipLevel = 2;
        int width = 1920 >> mipLevel;
        int height = 1080 >> mipLevel;
        float[] depthTexture = new float[width * height];
        GL13.glBindTexture(GL13.GL_TEXTURE_2D, depth);
        GL13.glGetTexImage(GL13.GL_TEXTURE_2D, mipLevel, GL13.GL_DEPTH_COMPONENT, GL13.GL_FLOAT, depthTexture);
        //convert to linear depth
        //TODO: make linear depth conversion faster
        float depthAtCenter = -toLinearDepth(new Vector2f(0.5f, 0.5f), depthTexture[(height / 2) * width + width / 2]);
        float focusPoint = depthAtCenter * lerpSpeed + lastFocusPoint * (1f - lerpSpeed);
        lastFocusPoint = focusPoint;
        float focusRange = Math.max(100, focusPoint / 2);
        downSampleShader.loadFloat("focusPoint",focusPoint);
        downSampleShader.loadFloat("focusRange",focusRange);
    }

    private float toLinearDepth(Vector2f TexCoord, float depth) {
        float z = depth * 2.0f - 1.0f;
        Vector4f clipSpacePosition = new Vector4f(TexCoord.x * 2.0f - 1.0f, TexCoord.y * 2f - 1f, z, 1.0f);
        Vector4f viewSpacePosition = projMatrixInv.transform(clipSpacePosition);
        viewSpacePosition.mul(1f / viewSpacePosition.w);
        return viewSpacePosition.z;
    }
*/
}
