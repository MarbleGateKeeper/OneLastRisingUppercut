package dev.marblegate.olru.client;

import dev.marblegate.olru.client.animation.ClientFirstPersonAnimator;
import dev.marblegate.olru.client.animation.ClientGauntletAnimations;
import dev.marblegate.olru.client.effect.ClientCameraEffects;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import dev.marblegate.olru.client.hud.GauntletHudRenderer;
import dev.marblegate.olru.client.hud.SedationOverlayRenderer;
import dev.marblegate.olru.client.movement.ClientMovementInteractionState;
import dev.marblegate.olru.client.movement.ClientMovementManager;
import dev.marblegate.olru.client.render.effect.GauntletClientEffectRenderers;
import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.attachment.GauntletEntityState;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.config.OLRUClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = OneLastRisingUppercut.MODID, dist = Dist.CLIENT)
public class OneLastRisingUppercutClient {
    public OneLastRisingUppercutClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(ModConfig.Type.CLIENT, OLRUClientConfig.SPEC);
        modEventBus.addListener(this::registerGuiLayers);
        modEventBus.addListener(this::registerEntityRenderers);
        modEventBus.addListener(this::addEntityRenderLayers);
        modEventBus.addListener(this::registerRenderStateModifiers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(this::onRenderArm);
        NeoForge.EVENT_BUS.addListener(ClientFirstPersonAnimator::onRenderHand);
        NeoForge.EVENT_BUS.addListener(ClientCameraEffects::onComputeFov);
        NeoForge.EVENT_BUS.addListener(ClientCameraEffects::onComputeCameraAngles);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gauntlet_hud"),
                new GauntletHudRenderer());
        event.registerAboveAll(
                Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "sedation_overlay"),
                new SedationOverlayRenderer());
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OLRUEntityTypes.BIOTIC_GRENADE.get(), ThrownItemRenderer::new);
    }

    private void addEntityRenderLayers(EntityRenderersEvent.AddLayers event) {
        GauntletClientEffectRenderers.addLayers(event);
    }

    private void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        GauntletClientEffectRenderers.registerRenderStateModifiers(event);
    }

    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientGauntletEffects.tick();
        ClientGauntletAnimations.tick();
        ClientCameraEffects.tick();
        if (mc.player == null || mc.level == null) {
            ClientMovementInteractionState.clear();
            return;
        }
        GauntletEntityState state = mc.player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        state.tick();
        ClientMovementManager.tick();
        if (mc.screen != null) {
            return;
        }
        ClientInputHandler.dispatchMovementRuntimeInput(mc);
    }

    private void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        ClientGauntletEffects.renderWorld(event);
    }

    private void onRenderArm(RenderArmEvent event) {
        GauntletClientEffectRenderers.renderFirstPersonArm(event);
    }
}
