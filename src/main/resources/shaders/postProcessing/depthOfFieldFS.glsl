#version 130

in vec2 uv;
out vec4 out_color;

uniform sampler2D color;
uniform sampler2D bluredColor;
uniform sampler2D depthTexture;

uniform float focusPoint;
uniform float focusRange;

uniform mat4 projMatrixInv;

const float transition = 1000;

//TODO: make linear depth conversion faster
vec3 viewPosFromDepth(vec2 TexCoord,float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(TexCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = projMatrixInv * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    return viewSpacePosition.xyz;
}

void main() {

    float d = texture(depthTexture,uv).r;
    float distanceFromCam = -viewPosFromDepth(uv,d).z;
    float f = clamp(abs((distanceFromCam - clamp(distanceFromCam,focusPoint-focusRange,focusPoint+focusRange))/transition),0,1);
    out_color = mix(texture(color,uv),texture(bluredColor,uv),f);
}
