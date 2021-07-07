#version 130

in vec3 pos;
out vec2 uv;

void main() {

    uv = vec2(pos.xy+1)/2.0;
    gl_Position = vec4(pos, 1);

}