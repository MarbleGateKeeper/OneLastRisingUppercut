package dev.marblegate.olru.common.animation;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum GauntletPoseType {
    PRIME_HAND_CANNON_RECOIL(0),
    ROCKET_PUNCH_CHARGE(1),
    ROCKET_PUNCH_FLIGHT(2),
    ROCKET_PUNCH_IMPACT(3),
    RISING_UPPERCUT(4),
    SEISMIC_SLAM_LEAP(5),
    SEISMIC_SLAM_LAND(6),
    METEOR_HOVER(7),
    METEOR_DIVE(8),
    METEOR_LAND(9),
    HORUS_BIOTIC_ROUND_RECOIL(10),
    FIELD_EXTRACTION_CHANNEL(11),
    FIELD_EXTRACTION_RELEASE(12),
    SEDATIVE_DART_FIRE(13),
    BIOTIC_GRENADE_THROW(14),
    NANO_SURGE_CAST(15),
    SEDATED_SLUMP(16);

    private final byte networkId;

    GauntletPoseType(int networkId) {
        this.networkId = (byte) networkId;
    }

    public byte networkId() {
        return networkId;
    }

    public static GauntletPoseType byNetworkId(byte networkId) {
        for (GauntletPoseType type : values()) {
            if (type.networkId == networkId) return type;
        }
        throw new IllegalArgumentException("Unknown gauntlet pose type id: " + networkId);
    }

    public static final StreamCodec<ByteBuf, GauntletPoseType> STREAM_CODEC = ByteBufCodecs.BYTE.map(GauntletPoseType::byNetworkId, GauntletPoseType::networkId);
}
