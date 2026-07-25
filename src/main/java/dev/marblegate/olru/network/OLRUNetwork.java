package dev.marblegate.olru.network;

import dev.marblegate.olru.network.payload.ClientboundGauntletEffectPayload;
import dev.marblegate.olru.network.payload.ClientboundGauntletPosePayload;
import dev.marblegate.olru.network.payload.ClientboundMovementTaskStatePayload;
import dev.marblegate.olru.network.payload.ClientboundStartMovementPayload;
import dev.marblegate.olru.network.payload.ClientboundStopMovementPayload;
import dev.marblegate.olru.network.payload.ServerboundGauntletChargeReleasePayload;
import dev.marblegate.olru.network.payload.ServerboundGauntletSkillPayload;
import dev.marblegate.olru.network.payload.ServerboundMovementResultPayload;
import dev.marblegate.olru.network.payload.ServerboundMovementTaskActionPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class OLRUNetwork {
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("3");

        registrar.playToServer(
                ServerboundGauntletSkillPayload.TYPE,
                ServerboundGauntletSkillPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ServerboundGauntletSkillPayload.handle(payload, ctx)));

        registrar.playToServer(
                ServerboundGauntletChargeReleasePayload.TYPE,
                ServerboundGauntletChargeReleasePayload.STREAM_CODEC,
                ServerboundGauntletChargeReleasePayload::handle);

        registrar.playToServer(
                ServerboundMovementResultPayload.TYPE,
                ServerboundMovementResultPayload.STREAM_CODEC,
                ServerboundMovementResultPayload::handle);

        registrar.playToServer(
                ServerboundMovementTaskActionPayload.TYPE,
                ServerboundMovementTaskActionPayload.STREAM_CODEC,
                ServerboundMovementTaskActionPayload::handle);

        registrar.playToClient(
                ClientboundGauntletEffectPayload.TYPE,
                ClientboundGauntletEffectPayload.STREAM_CODEC,
                ClientboundGauntletEffectPayload::handle);

        registrar.playToClient(
                ClientboundGauntletPosePayload.TYPE,
                ClientboundGauntletPosePayload.STREAM_CODEC,
                ClientboundGauntletPosePayload::handle);

        registrar.playToClient(
                ClientboundMovementTaskStatePayload.TYPE,
                ClientboundMovementTaskStatePayload.STREAM_CODEC,
                ClientboundMovementTaskStatePayload::handle);

        registrar.playToClient(
                ClientboundStartMovementPayload.TYPE,
                ClientboundStartMovementPayload.STREAM_CODEC,
                ClientboundStartMovementPayload::handle);

        registrar.playToClient(
                ClientboundStopMovementPayload.TYPE,
                ClientboundStopMovementPayload.STREAM_CODEC,
                ClientboundStopMovementPayload::handle);
    }
}
