#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec3 viewPos;
in vec2 uv;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

void main() {
    float time = GameTime * 2.6;
    float radius = length(uv);
    float angle = atan(uv.y, uv.x);
    float spiral = 0.5 + 0.5 * sin(angle * 7.0 - radius * 15.0 + time * 3.0);
    float broken = step(0.28, hash21(floor(vec2(angle * 4.0, radius * 10.0) + time * 0.25)));
    float flow = 0.5 + 0.5 * sin(uv.x * 32.0 - time * 5.0 + uv.y * 3.0);

    float ribbonMask = (1.0 - smoothstep(0.22, 1.0, abs(uv.y)))
            * smoothstep(0.0, 0.05, uv.x) * smoothstep(1.0, 0.92, uv.x);
    float radialMask = (1.0 - smoothstep(0.16, 1.0, radius)) * (0.35 + spiral * 0.65) * broken;
    float mask = max(ribbonMask * (0.38 + flow * 0.62), radialMask);

    vec4 color = vertexColor * ColorModulator;
    float energy = max(0.48 + flow * 1.45, 0.45 + spiral * 1.7);
    float fog = 1.0 - total_fog_value(
            sphericalVertexDistance, cylindricalVertexDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(color.rgb * energy * fog, color.a * mask);
}
