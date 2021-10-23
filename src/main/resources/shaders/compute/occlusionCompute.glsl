#version 430
layout(local_size_x = 32, local_size_y = 1) in;
layout(binding = 0) uniform sampler2D hiZ;

layout(std430, binding = 0) writeonly buffer resultBuffer{
    int results[];
};

layout(std430, binding = 1) readonly buffer matrixBuffer{
    mat4 transformations[];
};

uniform vec3 minAABB;
uniform vec3 dimension;

uniform mat4 projViewMatrix;
uniform float viewPortWidth;
uniform float viewPortHeight;
uniform int maxSize;

void main() {
    uint invocation = gl_GlobalInvocationID.x;
    if (invocation>=maxSize)return;
    mat4 transformationMatrix=transformations[invocation];
    vec3 minNDC = vec3(1000);
    vec3 maxNDC = vec3(0);
    for (int x=0;x<=1;x++){
        for (int y=0;y<=1;y++){
            for (int z=0;z<=1;z++){
                vec4 vertexPos = transformationMatrix * vec4(minAABB+dimension*vec3(x, y, z), 1);
                vec4 clipPos = projViewMatrix * vertexPos;
                clipPos.z = max(0,clipPos.z);
                vec3 ndcPos = clipPos.xyz/clipPos.w;
                minNDC = min(minNDC, ndcPos);
                maxNDC = max(maxNDC, ndcPos);
            }
        }
    }
    float minZ = minNDC.z;
    float fullScreenWidth  = (maxNDC.x-minNDC.x)*viewPortWidth;
    float fullScreenHeight = (maxNDC.y-minNDC.y)*viewPortHeight;

    int lod = int(ceil(log2(max(fullScreenWidth, fullScreenHeight))));
    int maxLod = textureQueryLevels(hiZ);
    lod = min(lod, maxLod);
    results[invocation] = 0;
    vec4 box = vec4(minNDC.xy/2.0+0.5, maxNDC.xy/2.0+0.5);
    float d1 = textureLod(hiZ, box.xy, lod).r;
    float d2 = textureLod(hiZ, box.xw, lod).r;
    float d3 = textureLod(hiZ, box.zy, lod).r;
    float d4 = textureLod(hiZ, box.zw, lod).r;
    float maximum = max(max(d1, d2), max(d3, d4));
    if (minZ<=maximum){
        results[invocation] = 1;
    }
}