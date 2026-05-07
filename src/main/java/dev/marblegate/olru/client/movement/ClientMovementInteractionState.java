package dev.marblegate.olru.client.movement;

public class ClientMovementInteractionState {
    private static boolean meteorStrikeHoverActive = false;

    public static boolean isMeteorStrikeHoverActive() {
        return meteorStrikeHoverActive;
    }

    public static void setMeteorStrikeHoverActive(boolean active) {
        meteorStrikeHoverActive = active;
    }

    public static void clear() {
        meteorStrikeHoverActive = false;
    }
}
