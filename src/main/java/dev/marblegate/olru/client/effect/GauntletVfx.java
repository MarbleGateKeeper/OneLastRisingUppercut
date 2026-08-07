package dev.marblegate.olru.client.effect;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Shared VFX palette and shader pipelines for the gauntlet skill effects. */
public final class GauntletVfx {
    // Moira (The Final Answer): damage purple / heal gold
    public static final int MOIRA_PURPLE = 0xB04AD8;
    public static final int MOIRA_DEEP = 0x8A2BE2;
    public static final int MOIRA_GOLD = 0xFFD75A;
    public static final int MOIRA_PALE = 0xEAC4FF;
    // Sigma (The Axiom): gravity violet
    public static final int SIGMA_VIOLET = 0x9B4DFF;
    public static final int SIGMA_DEEP = 0x7A5CFF;
    public static final int SIGMA_PALE = 0xD9CCFF;
    /** Near-black violet used for the Kinetic Grasp event horizon. */
    public static final int SIGMA_CORE = 0x0A0618;

    /** Like vanilla lightning (additive position+color) but without depth writes: transparent layers never z-fight or occlude clouds/particles. */
    public static final RenderPipeline GLOW_PIPELINE = RenderPipelines.LIGHTNING.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "pipeline/glow"))
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

    /** Additive energy shader: procedural flowing liquid noise over position+uv+color geometry. */
    public static final RenderPipeline ENERGY_PIPELINE = RenderPipelines.LIGHTNING.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "pipeline/energy"))
            .withVertexShader(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "core/energy"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "core/energy"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    /** Soft filament-and-mist shader for The Final Answer's biotic liquid. */
    public static final RenderPipeline BIOTIC_PIPELINE = ENERGY_PIPELINE.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "pipeline/biotic"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "core/biotic"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .build();

    /** Broken spiral and gravity-line shader for The Axiom's fields. */
    public static final RenderPipeline GRAVITY_PIPELINE = ENERGY_PIPELINE.toBuilder()
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "pipeline/gravity"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "core/gravity"))
            .build();

    /** Normal alpha-blended position+color, no depth write — for dark occluding shapes (event horizon). */
    public static final RenderPipeline DARK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "pipeline/dark"))
            .withVertexShader(Identifier.withDefaultNamespace("core/rendertype_lightning"))
            .withFragmentShader(Identifier.withDefaultNamespace("core/rendertype_lightning"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    public static final RenderType GAUNTLET_ENERGY = RenderType.create(
            "olru_gauntlet_energy",
            RenderSetup.builder(ENERGY_PIPELINE)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup());

    public static final RenderType GAUNTLET_DARK = RenderType.create(
            "olru_gauntlet_dark",
            RenderSetup.builder(DARK_PIPELINE)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup());

    public static final RenderType GAUNTLET_BIOTIC = RenderType.create(
            "olru_gauntlet_biotic",
            RenderSetup.builder(BIOTIC_PIPELINE)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup());

    public static final RenderType GAUNTLET_GRAVITY = RenderType.create(
            "olru_gauntlet_gravity",
            RenderSetup.builder(GRAVITY_PIPELINE)
                    .bufferSize(RenderType.SMALL_BUFFER_SIZE)
                    .sortOnUpload()
                    .createRenderSetup());

    private GauntletVfx() {}

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GLOW_PIPELINE);
        event.registerPipeline(ENERGY_PIPELINE);
        event.registerPipeline(BIOTIC_PIPELINE);
        event.registerPipeline(GRAVITY_PIPELINE);
        event.registerPipeline(DARK_PIPELINE);
    }

    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0f;
    }

    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0f;
    }

    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0f;
    }

    public static int mix(int a, int b, float t) {
        t = Math.clamp(t, 0f, 1f);
        int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
