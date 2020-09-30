#version 150

in vec2 uv;
out vec4 color;

uniform sampler2D positionTexture;
uniform sampler2D currentSSAO;
uniform sampler2D lastSSAO;
uniform sampler2D lastDepth;
uniform sampler2D velocity;

uniform mat4 invViewMatrix;
uniform mat4 reprojectMatrix;
uniform mat4 projMatrixInv;

const float DISTANCE_OFFSET=0.1;
const float KEEP_HISTORY_MAX = 0.9;

vec3 viewPosFromDepth(vec2 TexCoord,float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(TexCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = projMatrixInv * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    return viewSpacePosition.xyz;
}

void main() {

    vec3 pos = texture(positionTexture, uv).xyz;
    vec3 worldSpace = (invViewMatrix*vec4(pos,1.0)).xyz-(texture(velocity,uv).xyz);
    vec4 oldClipPos = reprojectMatrix*vec4(worldSpace, 1.0);
    vec2 oldPosUV = 0.5 * (oldClipPos.xy/oldClipPos.w)+0.5;
    vec3 prev_Pos = viewPosFromDepth(oldPosUV, texture(lastDepth,oldPosUV).r);
    float depth_similarity = clamp(pow(prev_Pos.z/pos.z, 4)-DISTANCE_OFFSET, 0.0, 1.0);
    depth_similarity*=KEEP_HISTORY_MAX;
    if (oldPosUV.x<0||oldPosUV.x>1||oldPosUV.y<0||oldPosUV.y>1)
    depth_similarity=0;
    color =texture(currentSSAO, uv)*(1.0-depth_similarity)+texture(lastSSAO, oldPosUV)*depth_similarity;
}
