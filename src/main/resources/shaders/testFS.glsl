#version 330 core
precision highp float;

in vec4 colorAndShininessPassed;
in vec3 viewPos;

layout (location = 0) out vec3 normalAndShininess;
layout (location = 1) out vec4 colorAndGeometryCheck;

vec2 encodeNormal(vec3 n){
float p = sqrt(n.z*8+8);
return vec2(n.xy/p + 0.5);
}

void main()  {
    vec3 xTangent = dFdx(viewPos);
    vec3 yTangent = dFdy(viewPos);
    vec3 Normal = normalize(cross(xTangent, yTangent));
    normalAndShininess = vec3(encodeNormal(Normal),colorAndShininessPassed.a);
    colorAndGeometryCheck=vec4(colorAndShininessPassed.rgb,1.0);
}