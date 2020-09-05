#version 330 core
precision highp float;

in vec4 colorAndShininessPassed;
in vec3 viewPos;
in vec3 toCamera;

layout (location = 0) out vec4 position;
layout (location = 1) out vec4 normalsAndShininess;
layout (location = 2) out vec4 colorAndGeometryCheck;

void main()  {
    vec3 xTangent = dFdx(viewPos);
    vec3 yTangent = dFdy(viewPos);
    vec3 viewSpaceNorm = normalize(cross(xTangent, yTangent));
    normalsAndShininess = vec4(viewSpaceNorm,colorAndShininessPassed.a);
    position = vec4(viewPos,1.0);
    colorAndGeometryCheck=vec4(colorAndShininessPassed.rgb,1.0);
}