#version 330 core

out vec4 ssao;
in vec2 uv;

uniform sampler2D viewPosTextureAndGeometryInfo;
uniform sampler2D normalAndShininessTexture;
uniform sampler2D texNoise;

const int kernelSize =11;
uniform vec3 kernel[kernelSize];

// parameters (you'd probably want to use them as uniforms to more easily tweak the effect)

float radius = 10;
float bias =2;

// tile noise texture over screen based on screen dimensions divided by noise size
const vec2 noiseScale = vec2(1920.0/4.0, 1080.0/4.0);

uniform mat4 projection;

float when_greaterThan(float a, float b){
    return max(sign(a-b),0.0);
}

void main()
{
    // get input for SSAO algorithm
    vec4 posAndGeom=texture(viewPosTextureAndGeometryInfo, uv);
    float occlusion = 0.0;
    if (posAndGeom.a>0.9){
        vec3 fragPos = posAndGeom.xyz;
        vec3 normal = normalize(texture(normalAndShininessTexture, uv).rgb);
        vec3 randomVec = normalize(texture(texNoise, uv * noiseScale).xyz*2.0-1.0);
        // iterate over the sampl kernel and calculate occlusion factor
        for (int i = 0; i < kernelSize; ++i)
        {
            // get sampl position
            vec3 sampl =reflect(kernel[i], randomVec);
            float dot = dot(sampl, normal);
            if (abs(dot)>0.15){
                sampl=sampl-when_greaterThan(0,dot)*2*sampl;
                sampl = fragPos + sampl * radius;

                // project sampl position (to sampl texture) (to get position on screen/texture)
                vec4 offset = vec4(sampl, 1.0);
                offset = projection * offset;// from view to clip-space
                offset.xyz /= offset.w;// perspective divide
                offset.xyz = offset.xyz * 0.5 + 0.5;// transform to range 0.0 - 1.0

                // get sampl depth
                vec4 posAndGeomSample = texture(viewPosTextureAndGeometryInfo, offset.xy);
                float samplDepth = posAndGeomSample.z;// get depth value of kernel sampl
                // range check & accumulate
                float rangeCheck = smoothstep(0.0, 1.0, radius / abs(fragPos.z - samplDepth));
                occlusion +=when_greaterThan(samplDepth, sampl.z + bias)*rangeCheck*posAndGeomSample.a;
            }
        }
    }
    occlusion = pow(1.0 - (occlusion / kernelSize), 32);
    ssao=vec4(vec3(occlusion), 1.0);
}