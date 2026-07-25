package dev.marblegate.olru.network.payload;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.item.AbstractGauntletItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent by the client when it releases a charged gauntlet skill one. Firing does not rely on the
 * server sharing the vanilla item-use state, so a client/server readiness desync at charge start
 * cannot silently swallow the release.
 */
public record ServerboundGauntletChargeReleasePayload(float chargePercent) implements CustomPacketPayload {
    public static final Type<ServerboundGauntletChargeReleasePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(OneLastRisingUppercut.MODID, "gauntlet_charge_release"));

    public static final StreamCodec<ByteBuf, ServerboundGauntletChargeReleasePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            ServerboundGauntletChargeReleasePayload::chargePercent,
            ServerboundGauntletChargeReleasePayload::new);

    public static void handle(ServerboundGauntletChargeReleasePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof AbstractGauntletItem gauntlet)) return;
            gauntlet.handleChargeRelease(player, payload.chargePercent());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
