#version 310 es
in vec3 aPosition;
out vec2 vTexCoord;
uniform mat4 uTransform;
void main()
{
    gl_Position = vec4(aPosition, 1.0);
    vTexCoord = (uTransform * vec4((aPosition.xy + vec2(1.0))*0.5, 0.0, 1.0)).xy;
}