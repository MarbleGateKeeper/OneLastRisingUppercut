package dev.marblegate.olru.common.attachment;

import dev.marblegate.olru.common.item.AbstractGauntletItem;
import dev.marblegate.olru.common.registry.OLRUAttachments;
import dev.marblegate.olru.common.registry.OLRUItems;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class GauntletStateSyncHandler {
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureKnownGauntletStateSynced(player);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureKnownGauntletStateSynced(player);
        }
    }

    public static void ensureKnownGauntletStateSynced(ServerPlayer player) {
        GauntletEntityState state = player.getData(OLRUAttachments.GAUNTLET_STATE.get());
        boolean changed = false;

        changed |= ensureGauntletState(player, state, OLRUItems.LEGACY_PRIME::get);
        changed |= ensureGauntletState(player, state, OLRUItems.LEGACY_OF_HORUS::get);

        if (changed) {
            player.setData(OLRUAttachments.GAUNTLET_STATE.get(), state);
        }
    }

    private static boolean ensureGauntletState(
            ServerPlayer player, GauntletEntityState state, Supplier<? extends AbstractGauntletItem> itemSupplier) {
        AbstractGauntletItem gauntlet = itemSupplier.get();
        boolean createIfMissing = hasGauntlet(player, gauntlet);
        return state.ensureConfigBound(gauntlet.gauntletId(), gauntlet::createDefaultSkillGroup, createIfMissing);
    }

    private static boolean hasGauntlet(ServerPlayer player, AbstractGauntletItem gauntlet) {
        if (player.getMainHandItem().getItem() == gauntlet) return true;
        if (player.getOffhandItem().getItem() == gauntlet) return true;

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == gauntlet) return true;
        }
        return false;
    }
}
