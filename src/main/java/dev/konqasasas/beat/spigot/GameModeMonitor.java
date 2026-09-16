package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.roster.RosterService;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public final class GameModeMonitor implements Listener {
    private final BeatPlugin plugin;private final RosterService rosters;private final AdminAuthorizer admins;private final EventStateService states;private final ConfigurationFiles configuration;
    public GameModeMonitor(BeatPlugin plugin,RosterService rosters,AdminAuthorizer admins,EventStateService states,ConfigurationFiles configuration){this.plugin=plugin;this.rosters=rosters;this.admins=admins;this.states=states;this.configuration=configuration;}
    @EventHandler public void changed(PlayerGameModeChangeEvent event){if(rosters.current().participant(event.getPlayer().getUniqueId()).isEmpty()||!CompetitionSafetyListener.active(states.current().tournamentState())||!unexpected(event.getNewGameMode()))return;String warning=configuration.message("warnings.unexpected-game-mode","[BEAT] WARNING: {player} のGameModeが競技中に {mode} へ変更されました。自動復旧はしていません。",java.util.Map.of("player",event.getPlayer().getName(),"mode",event.getNewGameMode().name()));plugin.getLogger().log(Level.WARNING,warning);Bukkit.getOnlinePlayers().stream().filter(player->admins.isAdmin(player.getUniqueId())).forEach(player->player.sendMessage(warning));}
    static boolean unexpected(GameMode mode){return mode!=GameMode.ADVENTURE&&mode!=GameMode.SPECTATOR;}
}
