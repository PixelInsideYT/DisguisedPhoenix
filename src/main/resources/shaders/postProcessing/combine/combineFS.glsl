#version 130

uniform sampler2D colorTexture;
uniform sampler2D bloomTexture;

in vec2 uv;
out vec4 result;

void main() {
    result = texture(colorTexture,uv) + texture(bloomTexture,uv);
}
