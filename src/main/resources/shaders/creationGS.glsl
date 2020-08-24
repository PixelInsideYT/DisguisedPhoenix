#version 330 core

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

in vec3 worldPos[];
in vec3 vertexColor[];
in float cutHeight[];

out vec3 color;
out vec3 normal;
out vec3 barycentric;
out float discardHeight;

void main() {

    vec3 calculatedNormal = normalize(cross(worldPos[1]-worldPos[0], worldPos[2]-worldPos[0]));

    normal = calculatedNormal;
    barycentric = vec3(1, 0, 0);
    gl_Position = gl_in[0].gl_Position;
    color = vertexColor[0];
    discardHeight=cutHeight[0];
    EmitVertex();

    normal = calculatedNormal;
    barycentric = vec3(0, 1, 0);
    gl_Position = gl_in[1].gl_Position;
    color = vertexColor[1];
    discardHeight=cutHeight[1];
    EmitVertex();

    normal = calculatedNormal;
    barycentric = vec3(0, 0, 1);
    gl_Position = gl_in[2].gl_Position;
    color = vertexColor[2];
    discardHeight=cutHeight[2];
    EmitVertex();

    EndPrimitive();

}
