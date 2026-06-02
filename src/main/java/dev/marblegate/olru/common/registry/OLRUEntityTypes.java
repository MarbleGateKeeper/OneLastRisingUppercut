package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.entity.BioticGrenade;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OLRUEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
            Registries.ENTITY_TYPE, OneLastRisingUppercut.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BioticGrenade>> BIOTIC_GRENADE = ENTITY_TYPES.register("biotic_grenade", () -> EntityType.Builder
            .<BioticGrenade>of(BioticGrenade::new, MobCategory.MISC)
            .sized(0.35F, 0.35F)
            .clientTrackingRange(64)
            .updateInterval(2)
            .build(ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "biotic_grenade"))));
}
