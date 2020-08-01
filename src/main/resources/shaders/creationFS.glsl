#version 130
precision highp float;

in vec3 normal;
in vec3 color;
in vec3 barycentric;

out vec4 FragColor;

uniform float percentage;

const float lineWidth =0.1;

float when_eq(float x, float y) {
    return 1.0 - abs(sign(x - y));
}

float edgeFactor(){
    vec3 d = fwidth(barycentric);
    vec3 a3 = smoothstep(vec3(0.0), d*lineWidth, barycentric);
    return min(min(a3.x, a3.y), a3.z);
}

const float solidifyTime = 0.9;

void main()  {
    float barycentricMin = min(min(barycentric.x, barycentric.y), barycentric.z);
    vec4 lineColor = vec4(color,1)* sign(1-edgeFactor());
    float fillStatus = min(max((percentage - solidifyTime)/(1-solidifyTime),0),1);
    FragColor = lineColor;
    FragColor += vec4(color*sign(fillStatus),sign(max(fillStatus*fillStatus-barycentricMin,0))*sign(fillStatus));
    if(FragColor.a < 0.01)discard;
}