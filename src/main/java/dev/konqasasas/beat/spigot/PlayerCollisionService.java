package dev.konqasasas.beat.spigot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Keeps player collision disabled on every scoreboard owned or used by BEAT. */
public final class PlayerCollisionService implements Listener {
    private static final String TEAM_NAME = "beat_nocollide";

    private final Plugin plugin;
    private final Set<Scoreboard> scoreboards =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public PlayerCollisionService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        var manager = Bukkit.getScoreboardManager();
        if (manager != null) prepare(manager.getMainScoreboard());
        for (Player player : Bukkit.getOnlinePlayers()) prepare(player.getScoreboard());
    }

    public void prepare(Scoreboard scoreboard) {
        scoreboards.add(scoreboard);
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) team = scoreboard.registerNewTeam(TEAM_NAME);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        for (Player player : Bukkit.getOnlinePlayers()) team.addEntry(player.getName());
    }

    public void show(Player player, Scoreboard scoreboard) {
        prepare(scoreboard);
        player.setScoreboard(scoreboard);
    }

    public void shutdown() {
        for (Scoreboard scoreboard : Set.copyOf(scoreboards)) {
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team != null) team.unregister();
        }
        scoreboards.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) return;
            prepare(event.getPlayer().getScoreboard());
            syncEntry(event.getPlayer().getName(), true);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        syncEntry(event.getPlayer().getName(), false);
    }

    private void syncEntry(String entry, boolean present) {
        for (Scoreboard scoreboard : scoreboards) {
            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) continue;
            if (present) team.addEntry(entry);
            else team.removeEntry(entry);
        }
    }
}
