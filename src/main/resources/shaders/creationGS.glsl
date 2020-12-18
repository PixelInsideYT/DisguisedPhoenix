#version 150

layout (triangles) in;
layout (triangle_strip, max_vertices = 3) out;

in vec3 viewPosIn[];
in vec4 vertexColor[];
in float cutHeight[];

out vec4 colorAndShininessPassed;
out vec3 barycentric;
out float discardHeight;
out vec3 viewPos;

void main() {
    barycentric = vec3(1, 0, 0);
    gl_Position = gl_in[0].gl_Position;
    colorAndShininessPassed = vertexColor[0];
    discardHeight=cutHeight[0];
    viewPos=viewPosIn[0];
    EmitVertex();

    barycentric = vec3(0, 1, 0);
    gl_Position = gl_in[1].gl_Position;
    colorAndShininessPassed = vertexColor[1];
    discardHeight=cutHeight[1];
    viewPos=viewPosIn[1];
    EmitVertex();

    barycentric = vec3(0, 0, 1);
    gl_Position = gl_in[2].gl_Position;
    colorAndShininessPassed = vertexColor[2];
    discardHeight=cutHeight[2];
    viewPos=viewPosIn[2];
    EmitVertex();

    EndPrimitive();

}
