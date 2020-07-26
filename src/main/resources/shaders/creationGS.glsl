#version 330 core

layout ( triangles ) in;
layout ( triangle_strip, max_vertices = 3 ) out;

in vec3 worldPos[];

out vec3 normal;
out vec3 barycentric;

void main() {

    vec3 calculatedNormal = normalize(cross(worldPos[1]-worldPos[0],worldPos[2]-worldPos[0]));

    normal = calculatedNormal;
    barycentric = vec3(1,0,0);
    gl_Position = gl_in[0].gl_Position;
    EmitVertex();

    normal = calculatedNormal;
    barycentric = vec3(0,1,0);
    gl_Position = gl_in[1].gl_Position;
    EmitVertex();

    normal = calculatedNormal;
    barycentric = vec3(0,0,1);
    gl_Position = gl_in[2].gl_Position;
    EmitVertex();

    EndPrimitive();

}
