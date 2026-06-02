package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundGauntletSkillPayload(
        SkillType skill) implements CustomPacketPayload {
    public enum SkillType {
        NORMAL_ATTACK, SKILL_ONE, SKILL_TWO, SKILL_THREE, ULTIMATE;

        private static final SkillType[] VALUES = values();

        public static SkillType byOrdinal(int i) {
            if (i < 0 || i >= VALUES.length) {
                throw new IllegalArgumentException("Unknown gauntlet skill ordinal: " + i);
            }
            return VALUES[i];
        }

        public static final StreamCodec<ByteBuf, SkillType> STREAM_CODEC = ByteBufCodecs.INT.map(SkillType::byOrdinal, Enum::ordinal);
    }

    public static final Type<ServerboundGauntletSkillPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gauntlet_skill"));

    public static final StreamCodec<FriendlyByteBuf, ServerboundGauntletSkillPayload> STREAM_CODEC = StreamCodec.composite(
            SkillType.STREAM_CODEC, ServerboundGauntletSkillPayload::skill,
            ServerboundGauntletSkillPayload::new);

    @Override
    public Type<ServerboundGauntletSkillPayload> type() {
        return TYPE;
    }

    public static ServerboundGauntletSkillPayload skillTwo() {
        return new ServerboundGauntletSkillPayload(SkillType.SKILL_TWO);
    }

    public static ServerboundGauntletSkillPayload skillThree() {
        return new ServerboundGauntletSkillPayload(SkillType.SKILL_THREE);
    }

    public static ServerboundGauntletSkillPayload ultimate() {
        return new ServerboundGauntletSkillPayload(SkillType.ULTIMATE);
    }

    public static ServerboundGauntletSkillPayload normalAttack() {
        return new ServerboundGauntletSkillPayload(SkillType.NORMAL_ATTACK);
    }

    public static void handle(ServerboundGauntletSkillPayload payload, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof AbstractGauntletItem gauntlet)) return;

        switch (payload.skill()) {
            case SKILL_ONE -> {}
            case SKILL_TWO -> gauntlet.performSkillTwo(player);
            case SKILL_THREE -> gauntlet.performSkillThree(player);
            case ULTIMATE -> gauntlet.performUltimate(player);
            case NORMAL_ATTACK -> gauntlet.performNormalAttack(player);
        }
    }
}
