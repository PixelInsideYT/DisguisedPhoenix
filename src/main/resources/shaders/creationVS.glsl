#version 130
precision highp float;
in vec4 pos;
in vec4 color;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform mat4 transformationMatrix;
uniform float modelHeight;
uniform float builtProgress;

out vec3 viewPosIn;
out vec4 vertexColor;
out float cutHeight;

void main(){
    vertexColor=color;
    cutHeight = modelHeight*builtProgress-pos.y;
    vec4 aPos = viewMatrix * transformationMatrix*vec4(pos.xyz, 1);
    viewPosIn = aPos.xyz;
    gl_Position =projMatrix*aPos;
}