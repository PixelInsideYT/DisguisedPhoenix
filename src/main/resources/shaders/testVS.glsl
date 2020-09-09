#version 140
precision highp float;

in vec4 posAndWobble;
in vec4 colorAndShininess;

uniform sampler2D noiseMap;
uniform float time;
uniform float lastDT;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform mat4 transformationMatrix;

out vec4 colorAndShininessPassed;
out vec3 viewPos;
out vec3 velocity;

void main(){
    colorAndShininessPassed=colorAndShininess;
    vec4 aPos = transformationMatrix*vec4(posAndWobble.xyz, 1);
    vec3 noise = (texture(noiseMap, aPos.xz*0.001+vec2(time, time+0.5)*0.1).xyz-0.5)*100;
    vec3 noiseLast = (texture(noiseMap, aPos.xz*0.001+vec2(time-lastDT, time-lastDT+0.5)*0.1).xyz-0.5)*100;
    velocity = (noise-noiseLast)*posAndWobble.w,1.0;
    aPos += vec4(noise, 0)*posAndWobble.w;
    vec4 bPos = viewMatrix*aPos;
    viewPos = bPos.xyz;
    gl_Position =projMatrix*bPos;
}