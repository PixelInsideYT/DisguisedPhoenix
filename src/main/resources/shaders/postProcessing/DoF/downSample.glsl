#version 130
in vec2 uv;
out vec4 downSampled;

uniform sampler2D inputTexture;
uniform sampler2D depthTexture;

uniform mat4 projMatrixInv;
uniform float focusPoint;
uniform float focusRange;
const float transition = 2000;


//TODO: make linear depth conversion faster
vec3 viewPosFromDepth(vec2 TexCoord,float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(TexCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = projMatrixInv * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    return viewSpacePosition.xyz;
}

float CoCFromUV(vec2 inUV){
    float d = texture(depthTexture,inUV).r;
    float distanceFromCam = -viewPosFromDepth(uv,d).z;
    return clamp(((distanceFromCam - clamp(distanceFromCam,focusPoint-focusRange,focusPoint+focusRange))/transition),0,1);
}

vec4 colorAndCoC(vec2 inUV){
    vec3 color = texture(inputTexture,inUV).rgb;
    float CoC = CoCFromUV(inUV);
    return vec4(color,CoC);
}

void main() {
    downSampled = colorAndCoC(uv);
}
