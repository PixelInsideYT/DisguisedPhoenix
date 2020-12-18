#version 150
#define RADIUS 6

// Gaussian filter values from the author's blurring shader
const float gaussian[RADIUS + 1] = float[](0.136498, 0.129188,0.109523,0.083173,0.056577,0.034474,0.018816);

uniform sampler2D ao_in;
uniform vec2 axis_f;
uniform int filter_scale;
uniform float edge_sharpness;

float unpackKey(vec2 p) {
    return p.x * (256.0 / 257.0) + p.y * (1.0 / 257.0);
}

out vec4 result;

void main(void){
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec3 val = texelFetch(ao_in, px, 0).xyz;
    float z_pos = unpackKey(val.yz);

    // Compute weighting for the term at the center of the kernel
    float base = gaussian[0];
    float weight = base;
    float sum = weight * val.x;
    ivec2 axis = ivec2(axis_f);

    for (int i = -RADIUS; i <= RADIUS; ++i){
        // We handle the center pixel above so skip that case
        if (i != 0){
            // Filter scale effects how many pixels the kernel actually covers
            ivec2 p = px + axis * i * filter_scale;
            vec3 sampl = texelFetch(ao_in, p, 0).xyz;
            float z = unpackKey(sampl.yz);
            float w =0.3 + gaussian[abs(i)];
            // Decrease weight as depth difference increases. This prevents us from
            // blurring across depth discontinuities
            w *= max(0.f, 1.f - (edge_sharpness * 2000.0) * abs(z_pos - z));
            sum += sampl.x * w;
            weight += w;
        }
    }
    float v = sum / (weight + 0.0001);
    result = vec4(v,val.y,val.z,1);
}