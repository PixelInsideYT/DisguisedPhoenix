#version 430

layout(local_size_x = 32, local_size_y = 18) in;
uniform mat4 projMatrixInv;
layout(binding = 0) uniform sampler2D depth_input;
layout(std430, binding = 1) buffer ssbo
{
    float focusPoint;
    float focusRange;
};
const float zNear = 1.0;
const float zFar = 100000.0;

float linearize_depth(float d){
    float z_n = 2.0 * d - 1.0;
    return 2.0 * zNear * zFar / (zFar + zNear - z_n * (zFar - zNear));
}

const float lerpSpeed = 1f;

shared int viewDepthTimesTen;
shared int sampleCounter;

void main() {
    if (gl_LocalInvocationIndex==0){
        viewDepthTimesTen = 0;
        sampleCounter=0;
    }
    barrier();
    vec2 uv = vec2(float(gl_GlobalInvocationID.x)/1920.0, float(gl_GlobalInvocationID.y)/1080.0);
    float samples = texture(depth_input, uv).r;
    //test if im in the sky
    if (samples!=1.0){
        atomicAdd(viewDepthTimesTen, int(linearize_depth(samples)));
        atomicAdd(sampleCounter, 1);
    }
    barrier();
    if (gl_LocalInvocationIndex==0&&sampleCounter!=0){
        float avgDepth=float(viewDepthTimesTen)/float(sampleCounter);
        focusPoint = avgDepth;//mix(focusPoint, avgDepth, lerpSpeed);
        focusRange = max(100, avgDepth/2.0);
    }
}
