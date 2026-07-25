package dev.marblegate.olru.client.render.effect;

import com.google.common.reflect.TypeToken;
import dev.marblegate.olru.client.animation.ClientGauntletAnimations;
import dev.marblegate.olru.client.effect.ClientGauntletEffects;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

public final class GauntletClientEffectRenderers {
    private GauntletClientEffectRenderers() {}

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new NanoSurgeRenderLayer<>(renderer));
                renderer.addLayer(new RocketPunchChargeRenderLayer<>(renderer));
            }

            AvatarRenderer<ClientMannequin> mannequinRenderer = event.getMannequinRenderer(skin);
            if (mannequinRenderer != null) {
                mannequinRenderer.addLayer(new NanoSurgeRenderLayer<>(mannequinRenderer));
                mannequinRenderer.addLayer(new RocketPunchChargeRenderLayer<>(mannequinRenderer));
            }
        }

        for (var entityType : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer(entityType);
            if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {
                addGauntletEffectLayers(livingRenderer);
            }
        }
    }

    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (LivingEntity entity, LivingEntityRenderState state) -> state.setRenderData(
                        NanoSurgeRenderData.KEY,
                        ClientGauntletEffects.nanoSurgeRenderData(entity.getId())));
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
                (LivingEntity entity, LivingEntityRenderState state) -> state.setRenderData(
                        RocketPunchChargeRenderData.KEY,
                        ClientGauntletEffects.rocketPunchChargeRenderData(entity.getId())));
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
                ClientGauntletAnimations.extractInto(avatar, renderState);
            }
        });
    }

    public static void renderFirstPersonArm(RenderArmEvent event) {
        RocketPunchChargeRenderData data = ClientGauntletEffects.rocketPunchChargeRenderData(event.getPlayer().getId());
        if (data != null) {
            RocketPunchChargeRenderLayer.submitFirstPersonRightArm(event, data);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void addGauntletEffectLayers(LivingEntityRenderer<?, ?, ?> renderer) {
        renderer.addLayer(new NanoSurgeRenderLayer(renderer));
        renderer.addLayer(new RocketPunchChargeRenderLayer(renderer));
    }
}
