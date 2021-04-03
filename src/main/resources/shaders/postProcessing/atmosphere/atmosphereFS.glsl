#version 150
#VAR numInScatterPoints
#VAR numOpticalDepthPoints
in vec2 uv;
in vec3 viewDir;
out vec4 color;

const float G_SCATTERING=-0.99f;
const float mieStrength = 10f;

uniform sampler2D originalTexture;
uniform sampler2D depthTexture;
uniform sampler2D noiseTexture;
uniform sampler2D lookUpTexture;

uniform sampler2D shadowTexture;
uniform mat4 toShadowMapCoords;

uniform vec3 camPos;
uniform vec3 scatterCoefficients;
uniform float atmosphereRadius;
uniform float planetRadius;
uniform float densityFalloff;
uniform mat4 invProjMatrix;
uniform float zNear;
uniform float zFar;
uniform vec3 dirToSun;

vec2 raySphere(vec3 center, float radius, vec3 rayOrigin, vec3 rayDir){
    vec3 offset = rayOrigin - center;
    float a = dot(rayDir,rayDir);
    float b = 2 * dot(offset, rayDir);
    float c = dot(offset,offset) - radius * radius;
    float discriminant = b*b-4*a*c;
    if(discriminant>0){
        float s = sqrt(discriminant);
        float dstToSphereNear = max(0, (-b-s)/(2*a));
        float dstToSphereFar = (s-b)/(2*a);
        if(dstToSphereFar>=0){
            return vec2(dstToSphereNear,dstToSphereFar-dstToSphereNear);
        }
    }
    return vec2(100000,0);
}

float densityAtPoint(vec3 densitySamplePoint){
    float heightAboveSurface = length(densitySamplePoint)-planetRadius;
    float height01 = heightAboveSurface /(atmosphereRadius-planetRadius);
    float localDensity = exp(-height01 * densityFalloff)*(1-height01);
    return localDensity;
}
/*
float opticalDepth(vec3 rayOrigin, vec3 rayDir, float rayLength){
    vec3 densitySamplePoint = rayOrigin;
    float stepSize = rayLength / (numOpticalDepthPoints-1);
    float opticalDepth = 0;
    for(int i=0;i<numOpticalDepthPoints; i++){
        float localDensity = densityAtPoint(densitySamplePoint);
        opticalDepth += localDensity * stepSize;
        densitySamplePoint += rayDir * stepSize;
    }
    return opticalDepth;
}*/

float opticalDepth(vec3 rayOrigin, vec3 rayDir){
    float height01Orig = (length(rayOrigin)-planetRadius) /(atmosphereRadius-planetRadius);
    float angle01 = (dot(normalize(rayOrigin),rayDir)*0.5+0.5);
    return texture(lookUpTexture,vec2(angle01,height01Orig)).r;
}

float opticalDepth2(vec3 rayOrigin, vec3 rayDir, float rayLength){
    vec3 endPoint = rayOrigin + rayDir * rayLength;
    float d = dot(rayDir, normalize(rayOrigin));
    const float blendStrength = 1.5;
    float w = clamp(d * blendStrength + .5,0,1);

    float d1 = opticalDepth(rayOrigin, rayDir) - opticalDepth(endPoint, rayDir);
    float d2 = opticalDepth(endPoint, -rayDir) - opticalDepth(rayOrigin, -rayDir);
    return mix(d2, d1, w);
}

float ComputeScattering(float lightDotView)
{
    float result = 1.0f - G_SCATTERING * G_SCATTERING;
    result /= (4.0f * 3.141592f * pow(1.0f + G_SCATTERING * G_SCATTERING - (2.0f * G_SCATTERING) *      lightDotView, 1.5f));
    return result;
}

vec3 calculateLight(vec3 rayOrigin, vec3 rayDir, float rayLength, vec3 originalColor){
    vec3 inScatterPoint = rayOrigin;
    float stepSize = rayLength/(numInScatterPoints-1);
    vec3 inScatteredLight = vec3(0);
    for(int i=0;i<numInScatterPoints;i++){
        //rayleigh
        float sunRayLength = raySphere(vec3(0), atmosphereRadius, inScatterPoint,dirToSun).y;
        float sunRayOpticalDepth = opticalDepth(inScatterPoint, dirToSun);
        float viewRayOpticalDepth = opticalDepth2(inScatterPoint, -rayDir,stepSize*i);
        vec3 transittance = exp(-(sunRayOpticalDepth+viewRayOpticalDepth)*scatterCoefficients);
        float localDensity = densityAtPoint(inScatterPoint);
        inScatteredLight += localDensity * transittance * stepSize*scatterCoefficients;
        inScatterPoint += rayDir * stepSize;
    }
    vec3 origColorTransmittance = exp(-opticalDepth2(rayOrigin, rayDir,rayLength)*scatterCoefficients);
    return origColorTransmittance*originalColor+inScatteredLight;
}

vec3 mieFog(vec3 rayOrigin, vec3 rayDir, vec3 originalColor, float maxTravel){
    int mieScatterPoints = 50;
    float rayLength=min(maxTravel,10000);
    vec3 accumFog = vec3(3);
    vec3 inScatterPoint = rayOrigin;
    float stepSize = rayLength/(mieScatterPoints-1);
    for(int i=0;i<mieScatterPoints;i++){
        //mie
        vec4 shadowMapPos = toShadowMapCoords*vec4(inScatterPoint, 1.0);
        shadowMapPos/=shadowMapPos.w;
        vec2 uvShadowMap = shadowMapPos.xy;
        float distanceFromLight = shadowMapPos.z;
        float shadowMapValue = texture(shadowTexture, uvShadowMap).r;
        if (shadowMapValue>distanceFromLight){
            accumFog += ComputeScattering(dot(rayDir,-dirToSun))*vec3(1f);
        }
        inScatterPoint += rayDir * stepSize;
    }
    accumFog=accumFog/mieScatterPoints;
    return accumFog;
}

float getSceneDistance(float depth){
    float z_n = 2.0 * depth - 1.0;
    return 2.0 * zNear * zFar / (zFar + zNear - z_n * (zFar - zNear));
}

void main() {
    vec3 originalColor = texture(originalTexture,uv).rgb;
    float sceneDepth = getSceneDistance(texture(depthTexture,uv).r);
    vec3 noise = texture(noiseTexture,uv*50).rgb;
    vec3 rayOrigin = camPos;
    vec3 rayDir = normalize(viewDir);

    vec2 hitInfo = raySphere(vec3(0),atmosphereRadius,rayOrigin,rayDir);
    float dstToAtmosphere = hitInfo.x;
    float dstThroughAtmosphere = min(hitInfo.y,sceneDepth-dstToAtmosphere);
    if(dstThroughAtmosphere>0){
        const float epsilon = 0.00001;
        vec3 pointInAtmosphere = rayOrigin + rayDir * (dstToAtmosphere+epsilon);
        vec3 light = calculateLight(pointInAtmosphere,rayDir,dstThroughAtmosphere-2*epsilon,originalColor)+mieFog(pointInAtmosphere,rayDir,originalColor,sceneDepth-dstToAtmosphere);
        light.rgb += (noise.r *2.0-1.0)/255.0;
        color = vec4(light, 1.0);
    }else{
        color = vec4(originalColor, 1.0);
    }
}
