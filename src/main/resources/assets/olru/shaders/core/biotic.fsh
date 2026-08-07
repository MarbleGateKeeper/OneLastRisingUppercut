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
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
            mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0)), f.x), f.y);
}

void main() {
    float time = GameTime * 3.4;
    vec2 p = vec2(uv.x * 5.0 - time, uv.y * 1.7);
    float warp = noise21(p * 1.7 + vec2(time * 0.13, -time * 0.08));
    float fine = noise21(p * 4.2 + vec2(-time * 0.4, time * 0.22));
    float filament = pow(0.5 + 0.5 * sin(p.x * 4.8 + warp * 8.0 + sin(p.y * 3.0)), 3.0);
    float body = 0.22 + warp * 0.48 + filament * 1.35 + fine * 0.18;

    float ribbonMask = (1.0 - smoothstep(0.28, 1.0, abs(uv.y)))
            * smoothstep(0.0, 0.06, uv.x) * smoothstep(1.0, 0.91, uv.x);
    float radial = 1.0 - smoothstep(0.18, 1.0, length(uv));
    float mask = max(ribbonMask, radial);
    float dissolve = smoothstep(0.18, 0.52, warp + fine * 0.25);
    float voxelSpark = step(0.93, hash21(floor((p + time * 0.05) * 3.0))) * 0.65;

    vec4 color = vertexColor * ColorModulator;
    float fog = 1.0 - total_fog_value(
            sphericalVertexDistance, cylindricalVertexDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(color.rgb * (body + voxelSpark) * fog, color.a * mask * (0.38 + 0.62 * dissolve));
}
