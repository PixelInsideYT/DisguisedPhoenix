#version 430 core
#define FAR_PLANE -1000.f

in vec2 uv;
out vec4 ao_out;

uniform sampler2D camera_positions;
uniform sampler2D camera_normals;

uniform int n_samples;
uniform int turns;
uniform float ball_radius;
uniform float sigma;
uniform float kappa;
uniform float beta;
uniform float rnd;

float when_lower_than(float x, float y) {
    return max(sign(y - x), 0.0);
}

void main(void){
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec4 current = texture(camera_positions, uv);
    //got the sky box
    // if(current.w==0)discard;
    vec3 pos =current.xyz;
    //vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));
    vec3 normal = normalize(texture(camera_normals, uv).xyz);
    // The Alchemy AO hash for random per-pixel offset
    float phi = (3 * px.x ^ px.y + px.x * px.y) * 10+rnd;
    const float TAU = 6.2831853071795864;
    const float ball_radius_sqr = pow(ball_radius, 2);
    // What's the radius of a 1m object at z = -1m to compute screen_radius properly?
    // Comments in their code mention we can compute it from the projection mat, or hardcode in like 500
    // and make the ball radius resolution dependent (as I've done currently)
    const float screen_radius = -ball_radius * 3500 / pos.z;
    int max_mip = textureQueryLevels(camera_positions) - 1;
    float ao_value = 0;
    for (int i = 0; i < n_samples; ++i){
        float alpha = 1.f / n_samples * (i + 0.5);
        float h = screen_radius * alpha;
        float theta = TAU * alpha * turns + phi;
        vec2 u = vec2(cos(theta), sin(theta));
        int m = clamp(findMSB(int(h)) - 4, 0, max_mip);
        ivec2 mip_pos = clamp((ivec2(h * u) + px) >> m, ivec2(0), textureSize(camera_positions, m) - ivec2(1));
        vec3 q = texelFetch(camera_positions, mip_pos, m).xyz;
        vec3 v = q - pos;
        // The original estimator in the paper, from Alchemy AO
        // I tried getting their new recommended estimator running but couldn't get it to look nice,
        // from taking a look at their AO shader it also looks like we compute this value quite differently
        ao_value += max(0, dot(v, normal)) / (dot(v, v) + 0.01);
    }
    // The original method in paper, from Alchemy AO
    ao_value = max(0, 1.f - 2.f * sigma / n_samples * ao_value);
    ao_value = pow(ao_value, kappa);

    // Do a little bit of filtering now, respecting depth edges
    if (abs(dFdx(pos.z)) < 0.02) {
        ao_value -= dFdx(ao_value) * ((px.x & 1) - 0.5);
    }
    if (abs(dFdy(pos.z)) < 0.02) {
        ao_value -= dFdy(ao_value) * ((px.y & 1) - 0.5);
    }
    ao_out = vec4(ao_value, ao_value,ao_value,1);
}