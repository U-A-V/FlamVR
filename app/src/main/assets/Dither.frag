#version 310 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform samplerExternalOES uTexture;

const float bayer4x4[16] = float[16](
0.0/16.0,  8.0/16.0,  2.0/16.0, 10.0/16.0,
12.0/16.0,  4.0/16.0, 14.0/16.0,  6.0/16.0,
3.0/16.0, 11.0/16.0,  1.0/16.0,  9.0/16.0,
15.0/16.0,  7.0/16.0, 13.0/16.0,  5.0/16.0
);

float dither4x4(vec2 uv) {
    ivec2 p = ivec2(mod(floor(uv * vec2(800.0,600.0)), 4.0));
    int index = p.x + p.y * 4;
    return bayer4x4[index];
}

void main()
{
    vec4 color = texture(uTexture, vTexCoord);
    float luminance = dot(vec3(0.2126, 0.7152, 0.0722), color.xyz);
    float threshold = dither4x4(vTexCoord);
    float ditheredValue = luminance > threshold ? 1.0 : 0.0;
    outColor = vec4(vec3(ditheredValue),1.0);
}