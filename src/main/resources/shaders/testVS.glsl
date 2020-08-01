#version 140

in vec3 pos;
in vec3 vertexColor;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform mat4 transformationMatrix;

out vec3 diffuse;
out vec3 toCamera;
out vec3 worldPos;
out vec3 viewPos;

void main(){
    diffuse=vertexColor;
    vec4 aPos = transformationMatrix*vec4(pos,1);
	worldPos = aPos.xyz;
	vec4 bPos = viewMatrix*aPos;
	viewPos = bPos.xyz;
	gl_Position =projMatrix*bPos;
	toCamera = (viewMatrix * vec4(0.0,0.0,0.0,1.0)).xyz - worldPos;
}