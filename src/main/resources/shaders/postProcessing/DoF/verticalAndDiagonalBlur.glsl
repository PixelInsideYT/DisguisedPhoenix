#version 140
#extension GL_ARB_separate_shader_objects : enable

const float PI = 3.141592;
const int NUM_SAMPLES = 16;

in vec2 uv;

layout (location = 0) out vec4 vertical;
layout (location = 1) out vec4 diagonal;


uniform sampler2D inputImage;

uniform vec2 invViewDimensions;

vec4 BlurTexture(sampler2D tex, vec2 input_uv, vec2 direction){
    vec4 finalColor = vec4(0.0);
    float blurAmount = 0.0f;
    vec2 antiOverlap_uv = input_uv + direction * 0.5f;
    for (int i = 0; i < NUM_SAMPLES; ++i){
        vec4 color = texture2D(tex, antiOverlap_uv + direction * i);
        color       *= color.a;
        blurAmount  += color.a;
        finalColor  += color;
    }
    return (finalColor / blurAmount);
}

void main() {

    float CoC = texture2D(inputImage, uv).a;
    if (CoC == 0.0){
        vertical = vec4(texture2D(inputImage,uv).rgb,CoC);
        diagonal = vec4(texture2D(inputImage,uv).rgb,CoC);
        return;
    } else {
        vec2 blurDirection = CoC * invViewDimensions * vec2(cos(PI/2.0), sin(PI/2.0));
        vertical = vec4(BlurTexture(inputImage, uv, blurDirection).rgb * CoC, CoC);
        vec2 blurDirection2 = CoC * invViewDimensions * vec2(cos(-PI/6.0), sin(-PI/6.0));
        diagonal = vec4(BlurTexture(inputImage, uv, blurDirection2).rgb * CoC + vertical.rgb, CoC); }
}