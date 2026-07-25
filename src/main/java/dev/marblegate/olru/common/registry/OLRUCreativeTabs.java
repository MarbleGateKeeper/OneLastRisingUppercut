package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OLRUCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OneLastRisingUppercut.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GAUNTLETS_TAB = TABS.register("gauntlets_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.olru.gauntlets"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> OLRUItems.LEGACY_PRIME.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(OLRUItems.LEGACY_PRIME.get());
                output.accept(OLRUItems.LEGACY_OF_HORUS.get());
                output.accept(OLRUItems.FINAL_ANSWER.get());
            })
            .build());
}
