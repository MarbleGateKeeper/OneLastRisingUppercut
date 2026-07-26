package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.entity.AccretionBoulderEntity;
import dev.marblegate.olru.common.entity.AxiomBarrierEntity;
import dev.marblegate.olru.common.entity.BioticGrenade;
import dev.marblegate.olru.common.entity.BioticOrbEntity;
import dev.marblegate.olru.common.entity.HypersphereEntity;
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

    public static final DeferredHolder<EntityType<?>, EntityType<BioticOrbEntity>> BIOTIC_ORB = ENTITY_TYPES.register("biotic_orb", () -> EntityType.Builder
            .<BioticOrbEntity>of(BioticOrbEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build(ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "biotic_orb"))));

    public static final DeferredHolder<EntityType<?>, EntityType<HypersphereEntity>> HYPERSPHERE = ENTITY_TYPES.register("hypersphere", () -> EntityType.Builder
            .<HypersphereEntity>of(HypersphereEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build(ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "hypersphere"))));

    public static final DeferredHolder<EntityType<?>, EntityType<AccretionBoulderEntity>> ACCRETION_BOULDER = ENTITY_TYPES.register("accretion_boulder", () -> EntityType.Builder
            .<AccretionBoulderEntity>of(AccretionBoulderEntity::new, MobCategory.MISC)
            .sized(0.9F, 0.9F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build(ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "accretion_boulder"))));

    // Entity sizes are static, so the barrier footprint is hardcoded to the config defaults
    // (4.0 x 2.5); gameplay logic reads the live config values instead.
    public static final DeferredHolder<EntityType<?>, EntityType<AxiomBarrierEntity>> AXIOM_BARRIER = ENTITY_TYPES.register("axiom_barrier", () -> EntityType.Builder
            .<AxiomBarrierEntity>of(AxiomBarrierEntity::new, MobCategory.MISC)
            .sized(4.0F, 2.5F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .build(ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "axiom_barrier"))));
}
