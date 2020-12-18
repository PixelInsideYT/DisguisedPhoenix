#version 430 core

out vec4 out_color;
in vec2 uv;

uniform sampler2D original;
uniform sampler2D blured;
uniform sampler2D depth;

uniform mat4 projMatrixInv;

layout(std430, binding = 1) buffer ssbo
{
    float focusPoint;
    float focusRange;
};
const float transition = 2000;

const float zNear = 1.0;
const float zFar = 100000.0;

float linearize_depth(float d){
    float z_n = 2.0 * d - 1.0;
    return 2.0 * zNear * zFar / (zFar + zNear - z_n * (zFar - zNear));
}

float CoCFromUV(vec2 inUV){
    float distanceFromCam = linearize_depth(texture(depth,inUV).r);
    return clamp(((distanceFromCam - clamp(distanceFromCam,focusPoint-focusRange,focusPoint+focusRange))/(focusRange/4.0)),0,1);
}

void main() {

    float CoC = CoCFromUV(uv);
    vec4 bluredColor = texture(blured, uv);
    vec4 originalColor = texture(original,uv);

    out_color = mix(originalColor,bluredColor,CoC);
}
