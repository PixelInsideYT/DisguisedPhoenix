#version 150

in vec2 uv;
out vec4 sRGB_out;

uniform sampler2D linearInputTexture;
// no need for gamma correction because GLFW takes care of that
const vec3 exposure = vec3(0.25);
void main() {
    vec3 linearColor = texture(linearInputTexture, uv).rgb;
    vec3 tone_mapped = vec3(1.0) - exp(-linearColor*exposure);
    sRGB_out = vec4(tone_mapped, 1.0);
}
