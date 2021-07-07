#version 150

in vec3 pos;

uniform mat4 matrix;

out vec2 screenSpaceUV;

void main() {
    gl_Position =matrix * vec4(pos, 1);
    vec3 ndc = gl_Position.xyz / gl_Position.w;
    screenSpaceUV = ndc.xy * 0.5 + 0.5;
}
