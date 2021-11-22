#version 140
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_explicit_attrib_location : enable

uniform sampler2D depthTexture;
uniform sampler2DArray shadowMapTexture;

uniform int shadowsEnabled;
uniform mat4 projMatrixInv;
uniform mat4 shadowReprojectionMatrix[4];
uniform float zFar;
uniform float splitRange[4];

in vec2 uv;
out vec4 FragColor;

vec3 viewPosFromDepth(vec2 TexCoord, float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(TexCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = projMatrixInv * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    return viewSpacePosition.xyz;
}

float when_gt(float x, float y) {
    return max(sign(x - y), 0.0);
}

const vec3[4] distanceColor=vec3[](vec3(1, 1, 0), vec3(1, 0, 0), vec3(0, 1, 0), vec3(0, 0, 1));

//TODO fix PCF filtering
const int pcfCount = 0;
const int totalTexels = (pcfCount*2+1)*(pcfCount*2+1);

float shadow(vec3 position, inout vec3 shadowColor){
    float shadow =1.0;
    int index =0;
    float linearDepth = -position.z/zFar;
    for (int i=0;i<4;i++){
        if (linearDepth<splitRange[i]){
            index=i;
            shadowColor = distanceColor[index];
            break;
        }
    }
    vec4 shadowMapPos = shadowReprojectionMatrix[index]*vec4(position, 1.0);
    vec2 uvShadowMap = shadowMapPos.xy;
    float distanceFromLight = shadowMapPos.z-0.001;

    ivec3 size =textureSize(shadowMapTexture, index);
    float texelSize = 1.0/float(size.x);
    float total =0.0;
    for (int x=-pcfCount;x<=pcfCount;x++){
        for (int y=-pcfCount;y<=pcfCount;y++){
            float shadowDistance = texture(shadowMapTexture, vec3(uvShadowMap+vec2(x,y)*texelSize, index)).r;
            total+=when_gt(distanceFromLight,shadowDistance);
        }
    }
    return 1-(total/totalTexels)*0.4;
}

void main(){
    vec3 FragPos = viewPosFromDepth(uv, texture(depthTexture, uv).r);
    float shadowMul = 1.0;
    vec3 shadowColor = vec3(0);
    if (shadowsEnabled==1){
        shadowMul=shadow(FragPos, shadowColor);
    }
    FragColor=vec4(vec3(shadowMul),1);
}