#version 140

uniform sampler2D toTest;

in vec2 uv;
out vec4 FragColor;

void main() {

    //float red = texture(toTest,uv).rgb;
    FragColor=vec4(texture(toTest,uv).rgb,1.0);

}
