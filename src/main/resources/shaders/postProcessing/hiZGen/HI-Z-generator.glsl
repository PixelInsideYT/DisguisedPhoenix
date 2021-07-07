#version 400 core

uniform sampler2D LastMip;
uniform ivec2 LastMipSize;
in vec2 uv;

void main(void) {
    vec4 texels;
    texels.x = texture(LastMip, uv).x;
    texels.y = textureOffset(LastMip, uv, ivec2(-1, 0)).x;
    texels.z = textureOffset(LastMip, uv, ivec2(-1, -1)).x;
    texels.w = textureOffset(LastMip, uv, ivec2(0, -1)).x;

    float maxZ = max(max(texels.x, texels.y), max(texels.z, texels.w));

    vec3 extra;
    // if we are reducing an odd-width texture then fetch the edge texels
    if (((LastMipSize.x & 1) != 0) && (int(gl_FragCoord.x) == LastMipSize.x-3)) {
        // if both edges are odd, fetch the top-left corner texel
        if (((LastMipSize.y & 1) != 0) && (int(gl_FragCoord.y) == LastMipSize.y-3)) {
            extra.z = textureOffset(LastMip, uv, ivec2(1, 1)).x;
            maxZ = max(maxZ, extra.z);
        }
        extra.x = textureOffset(LastMip, uv, ivec2(1, 0)).x;
        extra.y = textureOffset(LastMip, uv, ivec2(1, -1)).x;
        maxZ = max(maxZ, max(extra.x, extra.y));
    } else
    // if we are reducing an odd-height texture then fetch the edge texels
    if (((LastMipSize.y & 1) != 0) && (int(gl_FragCoord.y) == LastMipSize.y-3)) {
        extra.x = textureOffset(LastMip, uv, ivec2(0, 1)).x;
        extra.y = textureOffset(LastMip, uv, ivec2(-1, 1)).x;
        maxZ = max(maxZ, max(extra.x, extra.y));
    }
    gl_FragDepth = maxZ;
}