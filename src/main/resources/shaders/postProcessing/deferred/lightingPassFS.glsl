#version 140
#extension GL_ARB_separate_shader_objects : enable

uniform sampler2D depthTexture;
uniform sampler2D normalAndSpecularTexture;
uniform sampler2D colorAndGeometryCheckTexture;
uniform sampler2D ambientOcclusionTexture;

uniform mat4 projMatrixInv;
uniform float luminanceThreshold = 0.7;
uniform int fixAO;
in vec2 uv;

layout (location = 0) out vec4 FragColor;
layout (location = 1) out vec4 highLight;

uniform vec3 lightPos;

const vec3 lightColor = vec3(1, 1, 1);
const float gamma=1;
const vec3 luminanceDot = vec3(0.2126, 0.7152, 0.0722);

vec3 reconstructNormal(vec2 enc){
    vec2 fenc = enc*4-2;
    float f = dot(fenc, fenc);
    float g = sqrt(1-f/4);
    vec3 n;
    n.xy = fenc*g;
    n.z = 1-f/2;
    return n;
}

vec3 viewPosFromDepth(vec2 TexCoord,float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(TexCoord * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = projMatrixInv * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    return viewSpacePosition.xyz;
}
void main() {
    vec4 colorAndGeometryCheck = texture(colorAndGeometryCheckTexture, uv);
    if(colorAndGeometryCheck.w==0){
        FragColor = vec4(pow(colorAndGeometryCheck.rgb, vec3(1/gamma)),1.0);
        highLight = vec4(0,0,0,1);
        return;
    }
    vec3 FragPos = viewPosFromDepth(uv, texture(depthTexture,uv).r);
    vec3 normalAndShininess = texture(normalAndSpecularTexture, uv).xyz;
    float ambienOcclusion= texture(ambientOcclusionTexture, uv).r;
    if(fixAO==1)
    ambienOcclusion=1;
    float shininess = normalAndShininess.z;
    vec3 Normal = reconstructNormal(normalAndShininess.xy);
    vec3 Diffuse = colorAndGeometryCheck.rgb;
    float isGeometry=colorAndGeometryCheck.w;

    // blinn-phong (in view-space)
    vec3 ambient = vec3(0.3 * Diffuse * ambienOcclusion);// here we add occlusion factor
    vec3 lighting  = ambient;
    vec3 viewDir  = normalize(-FragPos);// viewpos is (0.0.0) in view-space
    // diffuse
    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 diffuse = max(dot(Normal, lightDir), 0.0) * Diffuse * lightColor;
    // specular
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(Normal, halfwayDir), 0.0), 0.0);
    vec3 specular = lightColor * spec;
    // attenuation
    float dist = length(lightPos - FragPos);
    float attenuation = 1.0 / (dist * dist);
    // diffuse  *= attenuation;
    // specular *= attenuation;
    lighting += diffuse;

    //calculate highlight for bloom post processing
    float luminance = dot(lighting, luminanceDot);
    highLight = vec4(lighting*pow(luminance,7),1);
    FragColor = vec4(lighting, 1);
    if(fixAO==0)
    FragColor.rgb = vec3(ambienOcclusion);
}
