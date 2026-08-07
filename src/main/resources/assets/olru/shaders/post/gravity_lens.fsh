#version 330

uniform sampler2D InSampler;
uniform sampler2D InDepthSampler;

layout(std140) uniform LensConfig {
    mat4 ProjMat;
    vec4 ScreenInfo; // x = width, y = height, z = time seconds, w = unused
    vec4 Counts; // x = active lens count
    vec4 LensA[16]; // xy = uv center, z = aspect-corrected uv radius, w = view depth (unused)
    vec4 LensB[16]; // x = strength, y = edge softness, z = chroma, w = aniso (vertical stretch)
    vec4 LensC[16]; // x = wobble amplitude, y = wobble frequency, z = bubble depth, w = ring boost
    vec4 LensTint[16]; // rgb = tint color, a = tint amount
};

in vec2 texCoord;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    float aspect = ScreenInfo.x / ScreenInfo.y;
    float time = ScreenInfo.z;
    float pixelDepth = texture(InDepthSampler, texCoord).r;

    vec2 totalOffset = vec2(0.0);
    float ringGlow = 0.0;
    vec3 tintAccum = vec3(0.0);
    float tintWeight = 0.0;
    float chromaAmt = 0.0;

    int count = int(Counts.x + 0.5);
    for (int i = 0; i < 16; i++) {
        if (i >= count) break;
        float radius = LensA[i].z;
        if (radius <= 0.0001) continue;
        // Occlusion: a pixel sitting noticeably closer than the bubble center is not warped.
        // The margin is loose on purpose — tight margins slice distant bubbles into half-circles.
        if (pixelDepth < LensC[i].z - 0.004) continue;

        vec2 d = texCoord - LensA[i].xy;
        d.x *= aspect;
        d.y /= max(LensB[i].w, 0.2);
        float dist = length(d);
        if (dist >= radius) continue;

        float soft = max(LensB[i].y, 0.001);
        float fall = 1.0 - smoothstep(radius * (1.0 - soft), radius, dist);
        float wobbleAmp = LensC[i].x;
        if (wobbleAmp > 0.0) {
            float n = hash12(floor(d * 220.0) + floor(time * LensC[i].y) * 0.731);
            fall *= 1.0 + (n - 0.5) * 2.0 * wobbleAmp;
        }
        vec2 dir = dist > 1.0e-4 ? d / dist : vec2(0.0);
        // Positive strength drags the background toward the center (gravitational pull); the constant
        // floor keeps distant pulses readable instead of shrinking to zero pixels
        float pull = LensB[i].x * fall * (radius * 0.6 + 0.012);
        totalOffset += dir * pull * vec2(1.0 / aspect, LensB[i].w);

        // Gravitational shear bends neighboring pixels around the well instead of only scaling them
        // radially. Its direction reverses with negative-strength fall fields, making lift and slam
        // read as opposite changes in space without adding more luminous geometry.
        vec2 tangent = vec2(-dir.y, dir.x);
        float shearWave = sin(atan(d.y, d.x) * 2.0 - dist / radius * 9.0 + time * LensC[i].y);
        totalOffset += tangent * pull * LensC[i].x * shearWave * 1.6
                * vec2(1.0 / aspect, LensB[i].w);

        float ringCenter = radius * 0.82;
        float ring = exp(-pow((dist - ringCenter) / (radius * 0.10), 2.0)) * LensC[i].w * fall;
        ringGlow += ring;

        float ta = LensTint[i].a * fall;
        tintAccum += LensTint[i].rgb * ta;
        tintWeight += ta;
        chromaAmt = max(chromaAmt, LensB[i].z * fall);
    }

    vec3 col;
    if (chromaAmt > 0.0) {
        col.r = texture(InSampler, texCoord + totalOffset * (1.0 + chromaAmt)).r;
        col.g = texture(InSampler, texCoord + totalOffset).g;
        col.b = texture(InSampler, texCoord + totalOffset * (1.0 - chromaAmt)).b;
    } else {
        col = texture(InSampler, texCoord + totalOffset).rgb;
    }
    col += ringGlow * vec3(0.85, 0.75, 1.0);
    if (tintWeight > 0.0) {
        col = mix(col, tintAccum / max(tintWeight, 1.0e-4), min(tintWeight, 0.55));
    }
    fragColor = vec4(col, 1.0);
}
