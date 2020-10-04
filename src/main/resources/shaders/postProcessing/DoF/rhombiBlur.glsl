#version 130

const float PI = 3.141592;
const int NUM_SAMPLES = 16;

in vec2 uv;
out vec4 blured;

uniform sampler2D verticalBlurTexture;
uniform sampler2D verticalAndDiagonalBlurTexture;

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

    float coc = texture(verticalBlurTexture, uv).a;
    vec2 blurDirection = coc * invViewDimensions * vec2(cos(-PI/6.0), sin(-PI/6.0));
    vec4 color = coc != 0.0 ? BlurTexture(verticalBlurTexture, uv, blurDirection) * coc : texture2D(verticalBlurTexture, uv);

    float coc2 = texture(verticalAndDiagonalBlurTexture, uv).a;
    vec2 blurDirection2 = coc2 * invViewDimensions * vec2(cos(-5.0*PI/6.0), sin(-5.0*PI/6.0));
    vec4 color2 = coc2 != 0.0 ? BlurTexture(verticalAndDiagonalBlurTexture, uv, blurDirection2) * coc2 : texture2D(verticalAndDiagonalBlurTexture, uv);

    vec3 result = color.rgb + color2.rgb;
    blured = vec4(result*0.5, 1.0);
}
