package dev.konqasasas.beat.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class BeatMenuHolder implements InventoryHolder {
    enum Screen {
        MAIN, START_HIGH, START_TA, START_ENDURANCE,
        PLAYERS, PLAYER_DETAIL, RESULTS, RESULT_HIGH, RESULT_TA, RESULT_ENDURANCE,
        OVERALL, WHITELIST, SETUP, VALIDATION, SETTINGS,
        EMERGENCY, CONFIRM_CANCEL, CONFIRM_COLLECT, CONFIRM_FORCE_END, CONFIRM_RESTART,
        CONFIRM_ANNOUNCE
    }
    private final Screen screen;
    private Inventory inventory;
    BeatMenuHolder(Screen screen) { this.screen = screen; }
    Screen screen() { return screen; }
    void inventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
