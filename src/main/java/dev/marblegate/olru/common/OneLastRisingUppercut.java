package dev.marblegate.olru.common;

import dev.marblegate.olru.common.attachment.GauntletEntityState;
import dev.marblegate.olru.common.attachment.GauntletStateSyncHandler;
import dev.marblegate.olru.common.core.FinalAnswerEffectTracker;
import dev.marblegate.olru.common.core.GauntletEventHandlers;
import dev.marblegate.olru.common.core.HorusEffectTracker;
import dev.marblegate.olru.common.core.movement.MovementManager;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.common.registry.OLRUCreativeTabs;
import dev.marblegate.olru.common.registry.OLRUEntityTypes;
import dev.marblegate.olru.common.registry.OLRUItems;
import dev.marblegate.olru.config.OLRUConfig;
import dev.marblegate.olru.network.OLRUNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@Mod(OneLastRisingUppercut.MODID)
public class OneLastRisingUppercut {
    public static final String MODID = "olru";

    public OneLastRisingUppercut(IEventBus modEventBus, ModContainer modContainer) {
        OLRUItems.ITEMS.register(modEventBus);
        OLRUEntityTypes.ENTITY_TYPES.register(modEventBus);
        OLRUCreativeTabs.TABS.register(modEventBus);
        OLRUAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(OLRUNetwork::onRegisterPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, OLRUConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(OneLastRisingUppercut::tickCooldowns);
        NeoForge.EVENT_BUS.addListener(GauntletStateSyncHandler::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(GauntletStateSyncHandler::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(MovementManager::tick);
        NeoForge.EVENT_BUS.addListener(MovementManager::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(MovementManager::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(HorusEffectTracker::onEntityTick);
        NeoForge.EVENT_BUS.addListener(HorusEffectTracker::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(HorusEffectTracker::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(HorusEffectTracker::onLivingHeal);
        NeoForge.EVENT_BUS.addListener(FinalAnswerEffectTracker::onEntityTick);
        NeoForge.EVENT_BUS.addListener(FinalAnswerEffectTracker::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(FinalAnswerEffectTracker::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GauntletEventHandlers::onLivingDamagePost);
    }

    public static void tickCooldowns(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof ServerPlayer player)) return;
        GauntletStateSyncHandler.ensureKnownGauntletStateSynced(player);
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        state.tick();
    }
}
