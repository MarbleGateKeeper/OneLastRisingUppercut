package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.item.FinalAnswerGauntletItem;
import dev.marblegate.olru.common.item.LegacyOfHorusGauntletItem;
import dev.marblegate.olru.common.item.LegacyPrimeGauntletItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OLRUItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OneLastRisingUppercut.MODID);

    public static final DeferredItem<LegacyPrimeGauntletItem> LEGACY_PRIME = ITEMS.register("legacy_prime",
            () -> new LegacyPrimeGauntletItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "legacy_prime"))).stacksTo(1)));

    public static final DeferredItem<LegacyOfHorusGauntletItem> LEGACY_OF_HORUS = ITEMS.register("legacy_of_horus",
            () -> new LegacyOfHorusGauntletItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "legacy_of_horus"))).stacksTo(1)));

    public static final DeferredItem<FinalAnswerGauntletItem> FINAL_ANSWER = ITEMS.register("final_answer",
            () -> new FinalAnswerGauntletItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "final_answer"))).stacksTo(1)));
}
