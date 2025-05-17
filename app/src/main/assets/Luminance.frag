#version 310 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

in vec2 vTexCoord;
out vec4 outColor;

uniform samplerExternalOES uTexture;
void main(){
    outColor = texture(uTexture, vTexCoord);
    outColor = vec4(vec3(0.2126*outColor.x + 0.7152*outColor.y + 0.0722*outColor.z), 1.0);
}