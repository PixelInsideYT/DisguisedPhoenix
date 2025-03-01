#version 140

in vec4 posAndWobble;
in vec4 colorAndShininess;
in mat4 transformationMatrix;

uniform sampler2D noiseMap;
uniform float time;
uniform mat4 viewProjMatrix;

void main(){
    vec4 aPos = transformationMatrix*vec4(posAndWobble.xyz, 1);
    vec3 noise = (texture(noiseMap, aPos.xz*0.1+vec2(time, time+0.5)*0.1).xyz-0.5);
    aPos += vec4(noise, 0)*posAndWobble.w;
    gl_Position = viewProjMatrix * aPos;
}