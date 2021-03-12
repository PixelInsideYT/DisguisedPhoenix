#version 150
#VAR numOpticalDepthPoints

in vec2 uv;
out vec4 color;

uniform float planetRadius;
uniform float densityFalloff;
uniform float atmosphereRadius;

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
}

void main() {
    float angle = acos(uv.x*2.0-1.0);
    vec3 dir = normalize(vec3(sin(angle),uv.x*2.0-1.0,0));
    vec3 samplePoint = vec3(0,uv.y*(atmosphereRadius-planetRadius)+planetRadius,0);
    vec2 hitInfo = raySphere(vec3(0),atmosphereRadius,samplePoint,dir);
    color = vec4(opticalDepth(samplePoint,dir,hitInfo.y),0,0,1);
}
