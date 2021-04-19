#version 430

layout(triangles, invocations = 4) in;
layout(triangle_strip, max_vertices = 3) out;
uniform mat4 viewProjMatrix[4];

void main() {
    for (int i=0;i<gl_in.length(); ++i){
        gl_Layer = gl_InvocationID;
        gl_Position = viewProjMatrix[gl_InvocationID] * gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}
