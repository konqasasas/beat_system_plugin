package dev.konqasasas.beat.spigot;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

/** Applies the gameplay mode only after Spigot has resolved a new player's spawn location. */
public final class FirstJoinGameModeListener implements Listener {
    private final FirstJoinTracker firstJoins = new FirstJoinTracker();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        firstJoins.recordSpawn(
                event.getPlayer().getUniqueId(),
                event.getPlayer().hasPlayedBefore());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        if (firstJoins.consumeJoin(event.getPlayer().getUniqueId())) {
            event.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
    }
}
