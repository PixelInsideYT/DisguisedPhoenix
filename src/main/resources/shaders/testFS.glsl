#version 330 core
precision highp float;

in vec4 colorAndShininessPassed;
in vec3 viewPos;
in vec3 velocity;

layout (location = 0) out vec4 positionAndGeometryCheck;
layout (location = 1) out vec4 normalsAndShininess;
layout (location = 2) out vec4 color;
layout (location = 3) out vec4 dx;


void main()  {
    vec3 xTangent = dFdx(viewPos);
    vec3 yTangent = dFdy(viewPos);
    vec3 viewSpaceNorm = normalize(cross(xTangent, yTangent));
    normalsAndShininess = vec4(viewSpaceNorm,colorAndShininessPassed.a);
    positionAndGeometryCheck = vec4(viewPos,1.0);
    color=vec4(colorAndShininessPassed.rgb,1.0);
    dx=vec4(velocity,1.0);
}