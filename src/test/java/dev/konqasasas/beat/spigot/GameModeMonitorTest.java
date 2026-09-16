package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

class GameModeMonitorTest {
    @Test void adventureAndEliminatedSpectatorAreValidButManualModesWarn(){
        assertFalse(GameModeMonitor.unexpected(GameMode.ADVENTURE));
        assertFalse(GameModeMonitor.unexpected(GameMode.SPECTATOR));
        assertTrue(GameModeMonitor.unexpected(GameMode.CREATIVE));
        assertTrue(GameModeMonitor.unexpected(GameMode.SURVIVAL));
    }
}
