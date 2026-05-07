package dev.marblegate.olru.client.movement.task;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public interface ClientMovementTask {
    /** Called each client tick. Return true when the task is complete. */
    boolean tick(LocalPlayer player, ClientLevel level);

    /**
     * Called when external runtime data is offered to the active movement task.
     * Return COMPLETE when the task handled the data and should be removed immediately.
     */
    default RuntimeDataResult onRuntimeData(LocalPlayer player, ClientLevel level, ClientMovementRuntimeData data) {
        return RuntimeDataResult.IGNORED;
    }

    /** Called when the task is cancelled externally (e.g. stop packet from server). */
    default void onCancelled(LocalPlayer player) {}

    enum RuntimeDataResult {
        IGNORED,
        CONSUMED,
        COMPLETE
    }
}
