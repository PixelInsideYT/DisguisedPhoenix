#version 140
in vec3 pos;

uniform mat4 viewMatrix;
uniform mat4 projMatrix;
uniform mat4 transformationMatrix;

void main(){
	gl_Position =projMatrix * viewMatrix* transformationMatrix * vec4(pos,1);
}