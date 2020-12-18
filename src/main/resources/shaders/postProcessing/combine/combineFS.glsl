#version 130

uniform sampler2D colorTexture;
uniform sampler2D bloomTexture;
uniform sampler2D godRaysTexture;

in vec2 uv;
out vec4 result;

void main() {
    result = vec4(texture(colorTexture,uv).rgb + texture(bloomTexture,uv).rgb + texture(godRaysTexture,uv).rgb,1.0);
}
