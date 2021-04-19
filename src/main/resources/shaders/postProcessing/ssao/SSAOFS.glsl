#version 150
#extension GL_ARB_gpu_shader5 : enable
#extension GL_ARB_explicit_attrib_location : enable
#extension GL_ARB_texture_query_levels : enable
#VAR samples
in vec2 uv;
out vec4 ao_out;

const float NUM_SPIRAL_TURNS = 11;
const float TWO_PI = 6.2831853071795864;

uniform sampler2D camera_positions;

uniform float kontrast = 5;
uniform float sigma = 5;
uniform float beta = 0.0005;
const float epsilon = 0.0001;


uniform mat4 projMatrixInv;
uniform float farPlane;

uniform float radius;
uniform float projScale;

vec3 viewPosFromDepth(vec2 TexCoord, float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(TexCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = projMatrixInv * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    return viewSpacePosition.xyz;
}

vec3 getPosition(ivec2 pos, int mipLevel, ivec2 size){
    float depth = texelFetch(camera_positions, pos, mipLevel).r;
    vec2 uvPos = (vec2(pos)+vec2(0.5))/vec2(size);
    return viewPosFromDepth(uvPos, depth);
}

vec2 packKey(float linearDepth) {
    float key = clamp(-linearDepth * (1.0/farPlane), 0.0, 1.0);
    // Round to the nearest 1/256.0
    float temp = floor(key * 256.0);
    vec2 p;
    // Integer part
    p.x = temp * (1.0 / 256.0);

    // Fractional part
    p.y = key * 256.0 - temp;
    return p;
}

float when_lt(float x, float y) {
    return max(sign(y - x), 0.0);
}

float calculateOcclusion(int i, vec3 pos, vec3 normal, ivec2 px, float hash, float diskRadius,int max_mip){
    //calculate sample point distance
    float alpha = float(i + 0.5)*(1.0/samples);
    float distance = diskRadius*alpha;
    // calculate sample point angle
    float theta = TWO_PI * alpha * NUM_SPIRAL_TURNS + hash;
    //calculate sample point in abselute texture coord
    ivec2 sampleTextureCoord = ivec2(vec2(cos(theta), sin(theta))*distance) + px;
    //calculate mipmap level and mipmap texture coord for chache effiecincy
    int mipLevel = clamp(findMSB(int(distance))-3, 0, max_mip);
    ivec2 size = textureSize(camera_positions, mipLevel);
    ivec2 mipRespectedSampleCooord = clamp(sampleTextureCoord >> mipLevel, ivec2(0), size-ivec2(1));
    vec3 P = getPosition(mipRespectedSampleCooord, mipLevel, size);
    vec3 v = P - pos;
    return max(0, dot(v, normal) + pos.z * beta)/(dot(v, v)+epsilon);
}

void main(void){
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec3 pos = getPosition(px, 0, textureSize(camera_positions, 0));
    vec3 normal = normalize(cross(dFdx(pos), dFdy(pos)));

    float diskRadius = -projScale * radius / pos.z;
    float hash = (3 * px.x ^ px.y + px.x * px.y) * 10;
    int max_mip = textureQueryLevels(camera_positions) - 1;
    float occlusion = 0;
    for (int i=0;i<samples;i++)
    occlusion+=calculateOcclusion(i, pos, normal, px, hash, diskRadius,max_mip);

    occlusion = max(0, 1.0 - 2.0 * sigma/samples*occlusion);
    occlusion = pow(occlusion, kontrast);
    if (abs(dFdx(pos.z)) < 0.02)
    occlusion -= dFdx(occlusion) * ((px.x & 1) - 0.5);//* when_lt(abs(dFdx(pos.z)), 0.02);
   if (abs(dFdy(pos.z)) < 0.02)
    occlusion -= dFdy(occlusion) * ((px.y & 1) - 0.5);//*  when_lt(abs(dFdy(pos.z)), 0.02);

    ao_out = vec4(occlusion, packKey(pos.z), 1);
    //ao_out.r = -pos.z/10000;
}