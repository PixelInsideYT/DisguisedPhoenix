#version 140

uniform sampler2D positionTexture;
uniform sampler2D normalTexture;
uniform sampler2D colorAndSpecularTexture;

in vec2 uv;
out vec4 FragColor;

uniform vec3 lightPos;

const vec3 lightColor = vec3(1, 1, 1);
const float gamma=1;


void main() {

    vec3 viewPos = texture(positionTexture, uv).xyz;
    vec4 normalAndShininess = texture(normalTexture, uv);
    vec4 colorAndGeometryCheck = texture(colorAndSpecularTexture, uv);

    vec3 toCamera = -viewPos;


    float shininess = normalAndShininess.w;
    vec3 norm = normalize(normalAndShininess.xyz);
    vec3 diffuse = colorAndGeometryCheck.rgb;
    vec3 lightDir = normalize(lightPos - viewPos);
    float isGeometry=colorAndGeometryCheck.w;

    float brightness = max((dot(norm, lightDir)+1)/2, 0.0);
    float ambient = 0.2;
    vec3 specular = diffuse;

    vec3 unitCamVec = normalize(toCamera);
    vec3 fromLightVector = - lightDir;
    vec3 reflectedLight = reflect(fromLightVector, norm);
    float specularFactor = pow(max(dot(reflectedLight, unitCamVec), 0), shininess);
    vec3 finalSpecular = max(specularFactor * specular * lightColor, vec3(0));
    vec3 resultingColor =  (ambient*lightColor+brightness*lightColor+finalSpecular)*diffuse;
    vec3 reinhardToneMapping = resultingColor /(resultingColor + vec3(1.0));
    vec3 gammaCorrected = pow(reinhardToneMapping, vec3(1.0 / gamma));
    float grey = (gammaCorrected.r+gammaCorrected.g+gammaCorrected.b)/3.0;
    FragColor=vec4(vec3(grey), 1);
    vec3 backGroundColorGammaCorrected = pow(diffuse,vec3(1/gamma))*(1.0-isGeometry);
    FragColor=vec4(gammaCorrected*isGeometry+backGroundColorGammaCorrected, 1);
}
