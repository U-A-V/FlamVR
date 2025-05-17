#version 310 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform samplerExternalOES uTexture;

void main() {
    vec2 texel = vec2(1.0 / 512.0, 1.0 / 512.0);

    // Sobel edge detection kernels
    float gx[9];
    float gy[9];

    gx[0] = -1.0; gx[1] = 0.0; gx[2] = 1.0;
    gx[3] = -2.0; gx[4] = 0.0; gx[5] = 2.0;
    gx[6] = -1.0; gx[7] = 0.0; gx[8] = 1.0;

    gy[0] = -1.0; gy[1] = -2.0; gy[2] = -1.0;
    gy[3] =  0.0; gy[4] =  0.0; gy[5] =  0.0;
    gy[6] =  1.0; gy[7] =  2.0; gy[8] =  1.0;

    float edgeX = 0.0;
    float edgeY = 0.0;
    int i = 0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y)) * texel;
            vec3 sampleColor = texture(uTexture, vTexCoord + offset).rgb;
            float gray = dot(sampleColor, vec3(0.299, 0.587, 0.114));
            edgeX += gray * gx[i];
            edgeY += gray * gy[i];
            i++;
        }
    }

    float edge = 1.0 - length(vec2(edgeX, edgeY));
    edge = smoothstep(0.2, 0.8, edge);

    outColor = vec4(vec3(edge), 1.0);
}