#version 150

uniform sampler2D image;
uniform vec2 blurCenter;
in vec2 uv;

out vec4 out_color;
const float blurFactor=1;
const float radius =0.001;
void main() {
    float samples = 32;
    vec4 col = vec4(0, 0, 0, 0);
    vec2 dist = uv - blurCenter;
    for (int j = 0; j < samples; j++) {
        float scale = 1 - blurFactor * (j / samples)* (clamp(length(dist) / radius, 0, 1.0));
        col += texture(image, dist * scale + blurCenter);
    }
    col /= samples;
    out_color = col;
}
