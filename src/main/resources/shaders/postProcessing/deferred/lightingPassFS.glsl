#version 140

uniform sampler2D positionTexture;
uniform sampler2D normalTexture;
uniform sampler2D colorAndSpecularTexture;
uniform sampler2D ambientOcclusionTexture;

in vec2 uv;
out vec4 FragColor;

uniform vec3 lightPos;

const vec3 lightColor = vec3(1, 1, 1);
const float gamma=1;


void main() {

    vec3 FragPos = texture(positionTexture, uv).xyz;
    vec4 normalAndShininess = texture(normalTexture, uv);
    vec4 colorAndGeometryCheck = texture(colorAndSpecularTexture, uv);
    float ambienOcclusion= texture(ambientOcclusionTexture, uv).r;

    float shininess = normalAndShininess.w;
    vec3 Normal = normalize(normalAndShininess.xyz);
    vec3 Diffuse = colorAndGeometryCheck.rgb;
    float isGeometry=colorAndGeometryCheck.w;

    // blinn-phong (in view-space)
    vec3 ambient = vec3(0.3 * Diffuse * ambienOcclusion); // here we add occlusion factor
    vec3 lighting  = ambient;
    vec3 viewDir  = normalize(-FragPos); // viewpos is (0.0.0) in view-space
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

    vec3 backGroundColorGammaCorrected = pow(Diffuse,vec3(1/gamma))*(1.0-isGeometry);
    FragColor=vec4(lighting*isGeometry+backGroundColorGammaCorrected, 1);
   // FragColor=texture(ambientOcclusionTexture, uv);
}
