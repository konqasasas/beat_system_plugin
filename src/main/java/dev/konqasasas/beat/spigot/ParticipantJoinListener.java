package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.application.ParticipantLoginResult;
import dev.konqasasas.beat.application.ParticipantService;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class ParticipantJoinListener implements Listener {
    private final BeatPlugin plugin;
    private final ParticipantService participants;
    private final AdminAuthorizer admins;

    public ParticipantJoinListener(
            BeatPlugin plugin,
            ParticipantService participants,
            AdminAuthorizer admins) {
        this.plugin = plugin;
        this.participants = participants;
        this.admins = admins;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        process(event.getPlayer());
    }

    public void process(Player player) {
        try {
            ParticipantLoginResult result = participants.handleLogin(
                    player.getUniqueId(), player.getName());
            if (result.registeredNameMismatch()) {
                String warning = "[BEAT] 登録MCIDと現在MCIDが異なります: "
                        + result.registeredMcid() + " -> " + player.getName();
                plugin.getLogger().warning(warning);
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(online -> admins.isAdmin(online.getUniqueId()))
                        .forEach(online -> online.sendMessage(warning));
            }
        } catch (PersistenceException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "大会時MCIDを安全に保存できなかったためBEATを停止します。",
                    exception);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }
}
