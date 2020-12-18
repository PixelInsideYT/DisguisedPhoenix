#version 150

uniform sampler2D geometryCheckTexture;
uniform vec3 color;

in vec2 screenSpaceUV;

out vec4 out_color;

void main() {
    float isGeometryInFront = texture(geometryCheckTexture,clamp(screenSpaceUV,vec2(0),vec2(1))).a;
    out_color = vec4(color*(1.0-isGeometryInFront),1.0);
}
