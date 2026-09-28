package dev.konqasasas.beat.ui;

import dev.konqasasas.beat.BeatPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Owns BEAT's Tab rows and explicitly synchronizes list order on Spigot 26.2. */
public final class PlayerListDisplayService implements Listener {
    private final BeatPlugin plugin;
    private final ProtocolPlayerVisibility packets;
    private final Map<UUID, Entry> entries = new HashMap<>();

    public PlayerListDisplayService(BeatPlugin plugin) {
        this.plugin = plugin;
        this.packets = new ProtocolPlayerVisibility(plugin);
    }

    public void set(Player player, String displayName, int order) {
        Entry next = new Entry(displayName, order);
        if (next.equals(entries.put(player.getUniqueId(), next))
                && displayName.equals(player.getPlayerListName())
                && order == player.getPlayerListOrder()) return;
        apply(player, next);
        packets.updateTab(player);
    }

    public void reset(UUID playerId) {
        entries.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        player.setPlayerListName(player.getName());
        player.setPlayerListOrder(0);
        packets.updateTab(player);
    }

    public void clearCompetition(Iterable<UUID> playerIds) {
        for (UUID playerId : playerIds) reset(playerId);
    }

    public void refreshViewer(Player viewer) {
        for (var row : entries.entrySet()) {
            Player target = Bukkit.getPlayer(row.getKey());
            if (target != null) packets.updateTab(viewer, target);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player joined = event.getPlayer();
            Entry own = entries.get(joined.getUniqueId());
            if (own != null) apply(joined, own);
            refreshViewer(joined);
            if (own != null) packets.updateTab(joined);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID targetId = event.getPlayer().getUniqueId();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(targetId)) packets.removeTab(viewer, targetId);
        }
    }

    public void shutdown() {
        for (UUID playerId : java.util.List.copyOf(entries.keySet())) reset(playerId);
        entries.clear();
    }

    private static void apply(Player player, Entry entry) {
        player.setPlayerListName(entry.displayName());
        player.setPlayerListOrder(entry.order());
    }

    private record Entry(String displayName, int order) { }
}
