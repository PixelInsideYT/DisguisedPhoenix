#version 140
in vec3 pos;
in mat4 transformationMatrix;
in vec4 color;

uniform mat4 viewMatrix;
uniform mat4 projMatrix;

out vec4 colorPass;

void main(){
    colorPass = color;
    gl_Position =projMatrix*viewMatrix*transformationMatrix*vec4(pos, 1);
}