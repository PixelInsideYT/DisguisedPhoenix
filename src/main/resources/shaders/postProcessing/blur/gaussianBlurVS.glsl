#version 140

in vec3 pos;
out vec2 uv[11];

uniform float targetSize;
uniform int horizontal;

void main() {

    gl_Position = vec4(pos,1);
    vec2 centerTextCoord = vec2(pos.xy+1)/2;
    float pixelSize = 1.0 / targetSize;
    for(int i=-5;i<5;i++){
        uv[i+5]=centerTextCoord+vec2(pixelSize*i*horizontal,pixelSize*i*(1.0-horizontal));
    }
}