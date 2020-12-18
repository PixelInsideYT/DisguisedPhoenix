#version 140

uniform sampler2D toTest;

in vec2 uv;
out vec4 FragColor;

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    //float red = texture(toTest,uv).rgb;
    FragColor=vec4(texelFetch(toTest,px,0).rrr,1.0);

}
