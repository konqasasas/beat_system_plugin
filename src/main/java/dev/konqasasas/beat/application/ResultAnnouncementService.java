package dev.konqasasas.beat.application;

import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;

public final class ResultAnnouncementService {
    private final OverallService results;
    private final ConfigurationFiles messages;

    public ResultAnnouncementService(OverallService results, ConfigurationFiles messages) {
        this.results = results;
        this.messages = messages;
    }

    public void announceCompetition(String kind) throws PersistenceException {
        var snapshot = results.current();
        switch (kind.toLowerCase(Locale.ROOT)) {
            case "high" -> {
                if (!snapshot.highConfirmed()) throw new IllegalStateException("高難易度の結果は未確定です");
                broadcast(message("commands.result.header-high", "{primary}--- HIGH DIFFICULTY RESULT ---{reset}"));
                snapshot.players().stream().filter(player -> player.high() != null && player.high().rank() != null)
                        .sorted(Comparator.comparingInt(player -> player.high().rank()))
                        .forEach(player -> broadcast(message("commands.result.row-high", "#{rank} {points}pt {player}", Map.of(
                                "rank", rank(player.high().rank()), "points", "%03d".formatted(player.high().points()),
                                "player", player.tournamentName()))));
            }
            case "ta" -> {
                if (!snapshot.timeAttackConfirmed()) throw new IllegalStateException("TAの結果は未確定です");
                broadcast(message("commands.result.header-ta", "{primary}--- TIME ATTACK RESULT ---{reset}"));
                snapshot.players().stream().filter(player -> player.timeAttack() != null)
                        .sorted(Comparator.comparingInt(player -> nullableRank(player.timeAttack().rank())))
                        .forEach(player -> broadcast(message("commands.result.row-ta", "#{rank} {time} {player}", Map.of(
                                "rank", rank(player.timeAttack().rank()),
                                "time", player.timeAttack().pbTicks() == null ? "--.--" : time(player.timeAttack().pbTicks()),
                                "player", player.tournamentName()))));
            }
            case "endurance" -> {
                if (!snapshot.enduranceConfirmed()) throw new IllegalStateException("耐久の結果は未確定です");
                broadcast(message("commands.result.header-endurance", "{primary}--- ENDURANCE RESULT ---{reset}"));
                snapshot.players().stream().filter(player -> player.endurance() != null)
                        .sorted(Comparator.comparingInt(player -> nullableRank(player.endurance().rank())))
                        .forEach(player -> broadcast(message("commands.result.row-endurance", "#{rank} Progress {progress} {player}", Map.of(
                                "rank", rank(player.endurance().rank()), "progress", "%03d".formatted(player.endurance().maxProgress()),
                                "player", player.tournamentName()))));
            }
            default -> throw new IllegalArgumentException("競技がhigh/ta/enduranceではありません");
        }
    }

    public void announceOverall() throws PersistenceException {
        var snapshot = results.current();
        if (!snapshot.overallConfirmed()) throw new IllegalStateException("総合結果は未確定です");
        broadcast(message("commands.overall.header", "{primary}--- OVERALL RESULT ---{reset}"));
        snapshot.players().stream().filter(player -> player.overall() != null)
                .sorted(Comparator.comparingInt(player -> player.overall().rank()))
                .forEach(this::broadcastOverallRow);
    }

    private void broadcastOverallRow(PlayerResultSnapshot player) {
        var overall = player.overall();
        broadcast(message("commands.overall.row", "#{rank} x{score} {player} [{high} * {ta} * {endurance}]", Map.of(
                "rank", "%02d".formatted(overall.rank()), "score", overall.scoreProduct(),
                "player", player.tournamentName(), "high", overall.highRank(),
                "ta", overall.taRank(), "endurance", overall.enduranceRank())));
    }

    private void broadcast(String message) { Bukkit.broadcastMessage(message); }
    private String message(String path, String fallback) { return messages.message(path, fallback); }
    private String message(String path, String fallback, Map<String, ?> values) { return messages.message(path, fallback, values); }
    private static int nullableRank(Integer rank) { return rank == null ? Integer.MAX_VALUE : rank; }
    private static String rank(Integer rank) { return rank == null ? "--" : "%02d".formatted(rank); }
    private static String time(long ticks) { return "%02d.%02d".formatted(ticks / 20, (ticks % 20) * 5); }
}
