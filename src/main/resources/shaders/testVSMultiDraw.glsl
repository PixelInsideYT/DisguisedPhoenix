#version 140
precision highp float;

in vec4 posAndWobble;
in vec4 colorAndShininess;
in mat4 transformationMatrix;

uniform sampler2D noiseMap;
uniform float time;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;

out vec4 colorAndShininessPassed;
out vec3 toCamera;
out vec3 worldPos;
out vec3 viewPos;

void main(){
    colorAndShininessPassed=colorAndShininess;
    vec4 aPos = transformationMatrix*vec4(posAndWobble.xyz, 1);
    vec3 noise = (texture(noiseMap, aPos.xz*0.001+vec2(time, time+0.5)*0.1).xyz-0.5)*100;
    aPos += vec4(noise, 0)*posAndWobble.w;
    worldPos = aPos.xyz;
    vec4 bPos = viewMatrix*aPos;
    viewPos = bPos.xyz;
    gl_Position =projMatrix*bPos;
    toCamera = (viewMatrix * vec4(0.0, 0.0, 0.0, 1.0)).xyz - worldPos;
}