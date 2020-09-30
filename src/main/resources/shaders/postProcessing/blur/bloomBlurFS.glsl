#version 150
#extension GL_EXT_gpu_shader4 : enable

out vec4 out_colour;

uniform sampler2D image;
uniform vec2 direction;
uniform int mipMapLevel;

void main(void){
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec4 color = vec4(0.0);
    ivec2 off1 = ivec2(direction);
    ivec2 off2 = ivec2(direction*2);
    ivec2 off3 = ivec2(direction*3);

    ivec2 size = textureSize(image, mipMapLevel);
    ivec2 coord = px;
    color += texelFetch(image, clamp(coord, ivec2(0), size-ivec2(1)), mipMapLevel)*0.214607;
    color += texelFetch(image, clamp(coord + off1, ivec2(0), size-ivec2(1)), mipMapLevel)*0.189879;
    color += texelFetch(image, clamp(coord - off1, ivec2(0), size-ivec2(1)), mipMapLevel)*0.189879;
    color += texelFetch(image, clamp(coord + off2, ivec2(0), size-ivec2(1)), mipMapLevel)*0.131514;
    color += texelFetch(image, clamp(coord - off2, ivec2(0), size-ivec2(1)), mipMapLevel)*0.131514;
    color += texelFetch(image, clamp(coord + off3, ivec2(0), size-ivec2(1)), mipMapLevel)*0.071303;
    color += texelFetch(image, clamp(coord - off3, ivec2(0), size-ivec2(1)), mipMapLevel)*0.071303;
    out_colour=color;
}
