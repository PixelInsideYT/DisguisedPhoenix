#version 140
precision highp float;
in vec3 pos;
in vec3 normals;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform mat4 transformationMatrix;

out vec3 worldPos;

void main(){
    vec4 aPos = transformationMatrix*vec4(pos, 1);
    gl_Position =projMatrix*viewMatrix*aPos;
    worldPos = aPos.xyz + normals*vec3(0);
}