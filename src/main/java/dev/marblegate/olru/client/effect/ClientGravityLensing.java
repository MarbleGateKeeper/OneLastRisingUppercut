package dev.marblegate.olru.client.effect;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Screen-space gravitational lensing: up to {@value #MAX_LENSES} dynamic "lens bubbles" per frame that
 * bend the rendered world (refraction + Einstein-ring rim + chromatic fringe + tint), driven by a custom
 * UBO and a two-pass frame graph executed at {@link RenderLevelStageEvent.AfterLevel}. Shared by the
 * Kinetic Grasp black hole, Hypersphere implosion pulses and Gravitic Flux phase fields.
 */
public final class ClientGravityLensing {
    private static final int MAX_LENSES = 16;
    private static final int UBO_SIZE = 64 + 16 + 16 + MAX_LENSES * 16 * 4;
    private static final List<Lens> LENSES = new ArrayList<>();

    private static final RenderPipeline LENS_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gravity_lens"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "post/gravity_lens"))
            .withSampler("InSampler")
            .withSampler("InDepthSampler")
            .withUniform("LensConfig", UniformType.UNIFORM_BUFFER)
            .build();
    private static final RenderPipeline COPY_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "lens_copy"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "post/lens_copy"))
            .withSampler("InSampler")
            .build();

    private static @Nullable MappableRingBuffer lensUbo;

    private ClientGravityLensing() {}

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(LENS_PIPELINE);
        event.registerPipeline(COPY_PIPELINE);
    }

    /** Constant-strength lens with default look; re-add every tick to keep it alive. */
    public static void addLens(Vec3 pos, float radius, float strength, int ticks) {
        addLens(-1, pos, radius, strength, 0.6f, 0.25f, 1.0f, 0, 0.0f, 0.0f, 0.0f, 1.0f, ticks);
    }

    /** Constant-strength lens bound to an entity's interpolated position; re-add every tick. */
    public static void addEntityLens(int entityId, Vec3 fallbackPos, float radius, float strength, int ticks) {
        addLens(entityId, fallbackPos, radius, strength, 0.6f, 0.25f, 1.0f, 0, 0.0f, 0.0f, 0.0f, 1.0f, ticks);
    }

    /** Full-parameter constant lens; re-add every tick to keep it alive. */
    public static void addLens(int entityId, Vec3 pos, float radius, float strength, float softness, float chroma,
            float aniso, int tintRgb, float tintAmount, float wobble, float wobbleFreq, float ring, int ticks) {
        Lens lens = new Lens();
        lens.entityId = entityId;
        lens.pos = pos;
        lens.radius = lens.endRadius = radius;
        lens.strength = strength;
        lens.softness = softness;
        lens.chroma = chroma;
        lens.aniso = aniso;
        lens.tintRgb = tintRgb;
        lens.tintAmount = tintAmount;
        lens.wobble = wobble;
        lens.wobbleFreq = wobbleFreq;
        lens.ring = ring;
        lens.maxScreenRadius = 0.24f;
        lens.totalTicks = lens.remainingTicks = Math.max(1, ticks);
        lens.pulse = false;
        if (LENSES.size() >= MAX_LENSES) LENSES.removeFirst();
        LENSES.add(lens);
    }

    /**
     * One-shot lensing pulse: a brief inward collapse (light sucked into the singularity) followed by an
     * expanding chromatic shockwave ring that decays to nothing.
     */
    public static void addPulse(Vec3 pos, float endRadius, float strength, int ticks) {
        for (Lens existing : LENSES) {
            if (existing.pulse
                    && existing.remainingTicks == existing.totalTicks
                    && existing.pos.distanceToSqr(pos) < 1.0E-6
                    && Math.abs(existing.endRadius - endRadius) < 1.0E-4f
                    && Math.abs(existing.strength - strength) < 1.0E-4f) {
                return;
            }
        }
        Lens lens = new Lens();
        lens.entityId = -1;
        lens.pos = pos;
        lens.radius = endRadius * 0.15f;
        lens.endRadius = endRadius;
        lens.strength = strength;
        lens.softness = 0.7f;
        lens.chroma = 0.12f;
        lens.aniso = 1.0f;
        lens.tintRgb = 0;
        lens.tintAmount = 0.0f;
        lens.wobble = 0.25f;
        lens.wobbleFreq = 6.0f;
        lens.ring = 0.55f;
        lens.maxScreenRadius = 0.20f;
        lens.totalTicks = lens.remainingTicks = Math.max(1, ticks);
        lens.pulse = true;
        if (LENSES.size() >= MAX_LENSES) LENSES.removeFirst();
        LENSES.add(lens);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LENSES.clear();
            return;
        }
        Iterator<Lens> iterator = LENSES.iterator();
        while (iterator.hasNext()) {
            if (--iterator.next().remainingTicks <= 0) iterator.remove();
        }
    }

    public static void render(RenderLevelStageEvent.AfterLevel event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        if (!camera.initialized) return;

        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float time = (mc.level.getGameTime() + partial) / 20.0f;
        RenderTarget main = mc.getMainRenderTarget();
        float aspect = (float) main.width / (float) main.height;
        boolean zZeroToOne = RenderSystem.getDevice().isZZeroToOne();

        float[] packed = new float[MAX_LENSES * 16];
        int count = 0;
        // Persistent phase fields are the visual anchor, so reserve the first slots for them.
        for (ClientGauntletEffects.LensView view : ClientGauntletEffects.transientLenses(mc.level)) {
            if (count >= MAX_LENSES) break;
            Lens lens = new Lens();
            lens.entityId = view.entityId();
            lens.pos = view.pos();
            lens.radius = lens.endRadius = view.radius();
            lens.strength = view.strength();
            lens.softness = view.softness();
            lens.chroma = view.chroma();
            lens.aniso = view.aniso();
            lens.tintRgb = view.tintRgb();
            lens.tintAmount = view.tintAmount();
            lens.wobble = view.wobble();
            lens.wobbleFreq = view.wobbleFreq();
            lens.ring = view.ring();
            lens.maxScreenRadius = view.maxScreenRadius();
            lens.totalTicks = lens.remainingTicks = 1;
            lens.pulse = false;
            if (projectLens(mc, camera, lens, partial, aspect, zZeroToOne, packed, count)) count++;
        }
        // One-shot implosion and impact pulses use any remaining slots.
        for (Lens lens : LENSES) {
            if (count >= MAX_LENSES) break;
            if (projectLens(mc, camera, lens, partial, aspect, zZeroToOne, packed, count)) count++;
        }
        if (count == 0) return;

        MappableRingBuffer ubo = lensUbo;
        if (ubo == null) {
            ubo = lensUbo = new MappableRingBuffer(() -> "olru LensConfig", 130, UBO_SIZE);
        }
        MappableRingBuffer uboFinal = ubo;

        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> mainHandle = frame.importExternal("main", main);
        ResourceHandle<RenderTarget> swapHandle = frame.createInternal(
                "olru_lens_swap", new RenderTargetDescriptor(main.width, main.height, false, 0));

        FramePass lensPass = frame.addPass("olru_gravity_lens");
        lensPass.reads(mainHandle);
        ResourceHandle<RenderTarget> swapOut = lensPass.readsAndWrites(swapHandle);
        int lensCount = count;
        lensPass.executes(() -> {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            RenderTarget swap = swapOut.get();
            try (GpuBuffer.MappedView mapped = encoder.mapBuffer(uboFinal.currentBuffer(), false, true)) {
                Std140Builder builder = Std140Builder.intoBuffer(mapped.data());
                builder.putMat4f(camera.projectionMatrix);
                builder.putVec4(main.width, main.height, time, 0f);
                builder.putVec4(lensCount, 0f, 0f, 0f);
                // GLSL declares LensA/LensB/LensC/LensTint as separate arrays, so upload each
                // field for all lenses before moving to the next field. The CPU-side array remains
                // grouped per lens to keep projectLens compact.
                for (int field = 0; field < 4; field++) {
                    for (int i = 0; i < MAX_LENSES; i++) {
                        int base = i * 16 + field * 4;
                        builder.putVec4(packed[base], packed[base + 1], packed[base + 2], packed[base + 3]);
                    }
                }
            }
            try (RenderPass pass = encoder.createRenderPass(
                    () -> "olru gravity lens", swap.getColorTextureView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
                pass.setPipeline(LENS_PIPELINE);
                pass.setUniform("LensConfig", uboFinal.currentBuffer());
                SamplerCache samplers = RenderSystem.getSamplerCache();
                pass.bindTexture("InSampler", main.getColorTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
                pass.bindTexture("InDepthSampler", main.getDepthTextureView(), samplers.getClampToEdge(FilterMode.NEAREST));
                pass.draw(0, 3);
            }
            uboFinal.rotate();
        });

        FramePass copyPass = frame.addPass("olru_lens_copy");
        copyPass.reads(swapOut);
        ResourceHandle<RenderTarget> mainOut = copyPass.readsAndWrites(mainHandle);
        copyPass.executes(() -> {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            RenderTarget swap = swapOut.get();
            RenderTarget output = mainOut.get();
            try (RenderPass pass = encoder.createRenderPass(
                    () -> "olru lens copy",
                    output.getColorTextureView(), OptionalInt.empty(),
                    output.useDepth ? output.getDepthTextureView() : null, OptionalDouble.empty())) {
                pass.setPipeline(COPY_PIPELINE);
                pass.bindTexture(
                        "InSampler", swap.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                pass.draw(0, 3);
            }
        });
        frame.execute(GraphicsResourceAllocator.UNPOOLED);
    }

    /** Projects one lens to screen space and packs its four vec4 rows; returns false when not visible. */
    private static boolean projectLens(Minecraft mc, CameraRenderState camera, Lens lens, float partial, float aspect,
            boolean zZeroToOne, float[] packed, int index) {
        Vec3 world = lens.pos;
        if (lens.entityId >= 0) {
            Entity entity = mc.level.getEntity(lens.entityId);
            if (entity != null) world = entity.getPosition(partial).add(0, entity.getBbHeight() * 0.5, 0);
        }

        float radius = lens.radius;
        float strength = lens.strength;
        if (lens.pulse) {
            float progress = Math.min((lens.totalTicks - lens.remainingTicks + partial) / lens.totalTicks, 1.0f);
            float collapseEnd = 0.22f;
            if (progress < collapseEnd) {
                float q = progress / collapseEnd;
                radius = lens.endRadius * Mth.lerp(q, 0.15f, 0.1f);
                strength = lens.strength * (1.0f - q * 0.4f);
            } else {
                float q = (progress - collapseEnd) / (1.0f - collapseEnd);
                radius = lens.endRadius * Mth.lerp(easeOutCubic(q), 0.1f, 1.0f);
                strength = -lens.strength * 1.4f * (1.0f - q) * (1.0f - q);
            }
        }

        Vec3 relative = world.subtract(camera.pos);
        Vector3f view = camera.viewRotationMatrix.transformPosition(
                new Vector3f((float) relative.x, (float) relative.y, (float) relative.z));
        if (view.z > -0.1f) return false; // behind the camera (view forward is -Z)

        Vector4f clip = new Vector4f(view, 1.0f).mul(camera.projectionMatrix);
        if (clip.w <= 0.0f) return false;
        float ndcX = clip.x() / clip.w;
        float ndcY = clip.y() / clip.w;
        float ndcZ = clip.z() / clip.w;
        if (Math.abs(ndcX) > 1.8f || Math.abs(ndcY) > 1.8f) return false;

        Vector4f clipEdge = new Vector4f(view.x + radius, view.y, view.z, 1.0f).mul(camera.projectionMatrix);
        if (clipEdge.w <= 0.0f) return false;
        float screenRadius = Math.abs(clipEdge.x() / clipEdge.w - ndcX) * 0.5f * aspect;
        screenRadius = Math.min(screenRadius, lens.maxScreenRadius);
        if (screenRadius * Math.max(aspect, 1.0f) < 0.002f) return false; // sub-pixel, skip

        float depth = zZeroToOne ? ndcZ : ndcZ * 0.5f + 0.5f;
        float u = ndcX * 0.5f + 0.5f;
        float v = ndcY * 0.5f + 0.5f;

        int base = index * 16;
        packed[base] = u;
        packed[base + 1] = v;
        packed[base + 2] = screenRadius;
        packed[base + 3] = -view.z;
        packed[base + 4] = strength;
        packed[base + 5] = lens.softness;
        packed[base + 6] = lens.chroma;
        packed[base + 7] = lens.aniso;
        packed[base + 8] = lens.wobble;
        packed[base + 9] = lens.wobbleFreq;
        packed[base + 10] = depth;
        packed[base + 11] = lens.ring;
        packed[base + 12] = ((lens.tintRgb >> 16) & 0xFF) / 255.0f;
        packed[base + 13] = ((lens.tintRgb >> 8) & 0xFF) / 255.0f;
        packed[base + 14] = (lens.tintRgb & 0xFF) / 255.0f;
        packed[base + 15] = lens.tintAmount;
        return true;
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static final class Lens {
        int entityId;
        Vec3 pos = Vec3.ZERO;
        float radius, endRadius, strength, softness, chroma, aniso, tintAmount, wobble, wobbleFreq, ring;
        float maxScreenRadius;
        int tintRgb;
        int totalTicks, remainingTicks;
        boolean pulse;
    }
}
