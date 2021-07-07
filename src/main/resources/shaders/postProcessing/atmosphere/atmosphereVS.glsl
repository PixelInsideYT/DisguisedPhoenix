#version 130

in vec3 pos;
out vec2 uv;
out vec3 viewDir;

uniform vec3 frustumRays[4];

void main() {
    uv = vec2(pos.xy+1)/2.0;
    viewDir = frustumRays[2*int(uv.y)+int(uv.x)];
    gl_Position = vec4(pos, 1);

}