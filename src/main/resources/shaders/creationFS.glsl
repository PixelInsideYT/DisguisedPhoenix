#version 150
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_attrib_location : enable

precision highp float;

in vec4 colorAndShininessPassed;
in vec3 barycentric;
in float discardHeight;
in vec3 viewPos;

layout (location = 0) out vec3 normalAndShininess;
layout (location = 1) out vec4 colorAndGeometryCheck;

uniform float builtProgress;

const float lineWidth =1;
const float solidifyTime = 0.9;

float when_eq(float x, float y) {
    return 1.0 - abs(sign(x - y));
}

float edgeFactor(){
    vec3 d = fwidth(barycentric);
    vec3 a3 = smoothstep(vec3(0.0), d*lineWidth, barycentric);
    return min(min(a3.x, a3.y), a3.z);
}

vec2 encodeNormal(vec3 n){
    float p = sqrt(n.z*8+8);
    return vec2(n.xy/p + 0.5);
}

void main()  {
    vec3 xTangent = dFdx(viewPos);
    vec3 yTangent = dFdy(viewPos);
    vec3 Normal = normalize(cross(xTangent, yTangent));
    float barycentricMin = min(min(barycentric.x, barycentric.y), barycentric.z);
    vec4 lineColor = vec4(colorAndShininessPassed.rgb, 1)* sign(1-edgeFactor());
    float fillStatus = min(max((builtProgress - solidifyTime)/(1-solidifyTime), 0), 1);
    vec4 FragColor=lineColor;
    FragColor += vec4(colorAndShininessPassed.rgb*sign(fillStatus), sign(max(fillStatus*fillStatus-barycentricMin, 0))*sign(fillStatus));
    if (FragColor.a < 0.01||discardHeight<0)discard;
    colorAndGeometryCheck = vec4(FragColor.rgb, 1.0);
    normalAndShininess = vec3(encodeNormal(Normal), colorAndShininessPassed.a);
}