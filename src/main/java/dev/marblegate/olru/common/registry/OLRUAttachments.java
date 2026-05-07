package dev.marblegate.olru.common.registry;

import dev.marblegate.olru.common.OneLastRisingUppercut;
import dev.marblegate.olru.common.attachment.GauntletEntityState;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class OLRUAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OneLastRisingUppercut.MODID);

    public static final Supplier<AttachmentType<GauntletEntityState>> GAUNTLET_STATE = ATTACHMENT_TYPES.register("gauntlet_state", () -> AttachmentType.builder(GauntletEntityState::new)
            .serialize(GauntletEntityState.CODEC)
            .sync(GauntletEntityState.STREAM_CODEC)
            .build());
}
