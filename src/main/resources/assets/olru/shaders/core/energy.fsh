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

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    float nx00 = mix(n000, n100, f.x);
    float nx10 = mix(n010, n110, f.x);
    float nx01 = mix(n001, n101, f.x);
    float nx11 = mix(n011, n111, f.x);
    return mix(mix(nx00, nx10, f.y), mix(nx01, nx11, f.y), f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += vnoise(p) * amp;
        p = p * 2.13 + vec3(11.7, 5.3, 8.9);
        amp *= 0.5;
    }
    return sum;
}

// Liquid energy: uv-anchored flow (streams run along uv.x), domain-warped fbm with bright
// filaments, valley erosion, and edge feathering so no quad outline is ever visible.
// Ribbon convention: uv.x = along 0..1, uv.y = across -1..1. Billboard convention: both -1..1.
void main() {
    vec3 q = vec3(uv.x * 3.0 - GameTime * 2.2, uv.y * 1.5, viewPos.z * 0.35 + viewPos.x * 0.1);
    float warp = vnoise(q * 1.5 + vec3(0.0, -GameTime * 1.1, 3.7));
    vec3 warped = q + (warp - 0.5) * 1.2;
    float n1 = fbm(warped);
    float n2 = fbm(warped * 2.4 + vec3(5.2, -GameTime * 3.1, 1.3));
    float field = clamp(n1 * 0.65 + n2 * 0.35, 0.0, 1.0);

    float body = 0.15 + 0.40 * field;
    float ridge = pow(field, 2.5) * 2.2;
    float erosion = smoothstep(0.16, 0.48, field);

    float ribbonMask = (1.0 - abs(uv.y)) * smoothstep(0.0, 0.10, uv.x) * smoothstep(1.0, 0.90, uv.x);
    float radialMask = smoothstep(1.0, 0.35, length(uv));
    float mask = max(ribbonMask, radialMask);

    vec4 color = vertexColor * ColorModulator;
    float fog = 1.0 - total_fog_value(
            sphericalVertexDistance, cylindricalVertexDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    fragColor = vec4(color.rgb * (body + ridge) * fog, color.a * erosion * mask);
}
