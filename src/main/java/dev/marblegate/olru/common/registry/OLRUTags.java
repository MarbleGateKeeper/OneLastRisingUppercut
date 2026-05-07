package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class OLRUTags {
    public static final Identifier PLAYER_ALLIES_ID = Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "player_allies");

    public static final TagKey<EntityType<?>> PLAYER_ALLIES = TagKey.create(
            Registries.ENTITY_TYPE,
            PLAYER_ALLIES_ID);
}
