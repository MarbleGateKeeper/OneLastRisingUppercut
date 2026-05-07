package dev.marblegate.olru.network.codec;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class OLRUStreamCodecs {
    public static final StreamCodec<ByteBuf, List<UUID>> UUID_LIST = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list());

    private OLRUStreamCodecs() {}
}
