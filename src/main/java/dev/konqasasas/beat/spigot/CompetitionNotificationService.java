package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.domain.notification.HighlightDecision;
import dev.konqasasas.beat.domain.notification.HighlightType;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class CompetitionNotificationService {
    private final AdminAuthorizer admins;
    private final ConfigurationFiles configuration;

    public CompetitionNotificationService(AdminAuthorizer admins, ConfigurationFiles configuration) {
        this.admins = admins;
        this.configuration = configuration;
    }

    public void highlight(Set<UUID> participants, UUID achiever, Collection<HighlightType> conditions,
            String label, String styledPlayer, String detail) {
        HighlightType primary = HighlightDecision.primary(conditions).orElseThrow();
        String message = formatHighlight(primary, label, styledPlayer, detail);
        forAudience(participants, player -> {
            player.sendMessage(message);
            if (!player.getUniqueId().equals(achiever)) play(player, soundPath(primary),
                    fallback(primary), 0.9F, pitch(primary));
        });
    }

    public void elimination(Set<UUID> participants, String detail, int count) {
        String amount = count == 0 ? configuration.message(
                "notifications.highlights.no-elimination", "対象者なし")
                : configuration.message("notifications.highlights.eliminated-count", "{count}人脱落",
                        Map.of("count", count));
        String tag = configuration.styleColor("highlight-tag-colors.elimination", ChatColor.RED)
                + "[脱落]" + ChatColor.RESET;
        String message = configuration.message("notifications.highlights.elimination", "{tag} {detail}｜{amount}",
                Map.of("tag", tag, "detail", detail, "amount", amount));
        forAudience(participants, player -> {
            player.sendMessage(message);
            play(player, "sounds.highlights.elimination", Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.7F, 0.7F);
        });
    }

    public String tag(HighlightType type, String label) {
        ChatColor fallback = switch (type) {
            case ALL_CLEAR, LEADER_CHANGE -> ChatColor.GOLD;
            case LEADER_UPDATE -> ChatColor.YELLOW;
            case GOAL -> ChatColor.GREEN;
            case COURSE_CLEAR, ZONE_2, ZONE_3 -> ChatColor.AQUA;
            case BORDER -> ChatColor.LIGHT_PURPLE;
            case PERSONAL_BEST -> ChatColor.GREEN;
            case RANK_CHANGE -> ChatColor.GRAY;
        };
        return configuration.styleColor("highlight-tag-colors." + type.styleKey(), fallback)
                + "[" + label + "]" + ChatColor.RESET;
    }

    public String formatHighlight(HighlightType type, String label, String styledPlayer, String detail) {
        return configuration.message("notifications.highlights.line", "{tag} {player}｜{detail}",
                Map.of("tag", tag(type, label), "player", styledPlayer, "detail", detail));
    }

    public void forAudience(Set<UUID> participants, java.util.function.Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (participants.contains(player.getUniqueId()) || admins.isAdmin(player.getUniqueId())) {
                action.accept(player);
            }
        }
    }

    private void play(Player player, String path, Sound fallback, float volume, float pitch) {
        player.playSound(player.getLocation(), configuration.sound(path, fallback),
                configuration.soundVolume(path, volume), configuration.soundPitch(path, pitch));
    }

    private static Sound fallback(HighlightType type) {
        return switch (type) {
            case ALL_CLEAR, GOAL -> Sound.UI_TOAST_CHALLENGE_COMPLETE;
            case LEADER_CHANGE -> Sound.ENTITY_PLAYER_LEVELUP;
            case LEADER_UPDATE, COURSE_CLEAR, ZONE_2, ZONE_3, BORDER, PERSONAL_BEST -> Sound.BLOCK_NOTE_BLOCK_PLING;
            case RANK_CHANGE -> Sound.BLOCK_NOTE_BLOCK_COW_BELL;
        };
    }

    private static String soundPath(HighlightType type) {
        return type == HighlightType.RANK_CHANGE
                ? "sounds.highlights.rank-change-notification"
                : "sounds.highlights." + type.styleKey();
    }

    private static float pitch(HighlightType type) {
        return switch (type) {
            case ALL_CLEAR, GOAL -> 1.0F;
            case LEADER_CHANGE -> 1.2F;
            case LEADER_UPDATE -> 1.4F;
            case COURSE_CLEAR, ZONE_2, ZONE_3, BORDER, PERSONAL_BEST -> 1.5F;
            case RANK_CHANGE -> 0.9F;
        };
    }
}
