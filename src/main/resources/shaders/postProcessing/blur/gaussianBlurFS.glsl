#version 130

out vec4 out_colour;
in vec2 uv;

uniform sampler2D image;
uniform vec2 resolution;
uniform vec2 direction;

void main(void){
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.411764705882353) * direction/ resolution;
    vec2 off2 = vec2(3.2941176470588234) * direction/ resolution;
    vec2 off3 = vec2(5.176470588235294) * direction/ resolution;
    color += texture2D(image, uv) * 0.1964825501511404;
    color += texture2D(image, uv + off1) * 0.2969069646728344;
    color += texture2D(image, uv - off1) * 0.2969069646728344;
    color += texture2D(image, uv + off2) * 0.09447039785044732;
    color += texture2D(image, uv - off2) * 0.09447039785044732;
    color += texture2D(image, uv + off3) * 0.010381362401148057;
    color += texture2D(image, uv - off3) * 0.010381362401148057;
    out_colour=color;
}
