#version 130
precision highp float;

uniform vec3 ambient;
uniform vec3 specular;

uniform float shininess;
uniform float opacity;

uniform mat4 viewMatrix;

in vec3 diffuse;
in vec3 worldPos;
in vec3 viewPos;
in vec3 toCamera;
out vec4 FragColor;

const vec3 lightPos = vec3(0,10000,1000);
const vec3 lightColor = vec3(1,1,1);
const float gamma=2.2;

void main()  {

    vec3 xTangent = dFdx( viewPos );
    vec3 yTangent = dFdy( viewPos );
    vec3 viewSpaceNorm = normalize( cross( xTangent, yTangent ) );
    vec3 norm = (transpose(viewMatrix) * vec4(viewSpaceNorm,0)).xyz;
    vec3 lightDir = normalize(lightPos - worldPos);

    float brightness = max(dot(norm,lightDir),0.0);

    vec3 unitCamVec = normalize(toCamera);
    vec3 fromLightVector = - lightDir;
    vec3 reflectedLight = reflect(fromLightVector, norm);
    float specularFactor = pow(max(dot(reflectedLight,unitCamVec),0),shininess);
    vec3 finalSpecular = max(specularFactor * specular * lightColor,vec3(0));
    vec3 resultingColor =  (ambient*lightColor+brightness*lightColor+finalSpecular)*diffuse;
    vec3 reinhardToneMapping = resultingColor /(resultingColor + vec3(1.0));
    vec3 gammaCorrected = pow(reinhardToneMapping, vec3(1.0 / gamma));
	FragColor=vec4(reinhardToneMapping,1);
	FragColor.a = opacity;
}