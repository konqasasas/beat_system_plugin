package dev.konqasasas.beat.command;

import dev.konqasasas.beat.application.OverallService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class OverallCommand {
    private final OverallService service;
    private final ConfigurationFiles messages;

    public OverallCommand(OverallService service, ConfigurationFiles messages) {
        this.service = service;
        this.messages = messages;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 1) { help(sender); return true; }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "calculate" -> {
                    var result = service.calculate();
                    long count = result.players().stream().filter(player -> player.overall() != null).count();
                    sender.sendMessage(message("calculated", "[BEAT] 総合順位を計算しました: {count}人", Map.of("count", count)));
                }
                case "confirm" -> { service.confirm(); sender.sendMessage(message("confirmed", "[BEAT] 総合結果を確定しました。")); }
                case "result" -> show(sender);
                default -> help(sender);
            }
        } catch (PersistenceException | IllegalStateException exception) {
            sender.sendMessage(messages.message("commands.error", "[BEAT] ERROR: {message}", Map.of("message", String.valueOf(exception.getMessage()))));
        }
        return true;
    }

    public List<String> tab(String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("calculate", "confirm", "result").stream().filter(value -> value.startsWith(prefix)).toList();
    }

    private void show(CommandSender sender) throws PersistenceException {
        var result = service.current();
        var rows = result.players().stream().filter(player -> player.overall() != null)
                .sorted(Comparator.comparingInt(player -> player.overall().rank())).toList();
        if (rows.isEmpty()) { sender.sendMessage(message("not-calculated", "[BEAT] 総合順位は未計算です。")); return; }
        sender.sendMessage(message("header", "{primary}--- OVERALL RESULT ---{reset}"));
        for (var player : rows) {
            var overall = player.overall();
            sender.sendMessage(message("row", "#{rank} x{score} {player} [{high} * {ta} * {endurance}]", Map.of(
                    "rank", "%02d".formatted(overall.rank()), "score", overall.scoreProduct(),
                    "player", player.tournamentName(), "high", overall.highRank(),
                    "ta", overall.taRank(), "endurance", overall.enduranceRank())));
        }
        String state = message(result.overallConfirmed() ? "confirmed-state" : "unconfirmed-state", result.overallConfirmed() ? "確定" : "未確定");
        sender.sendMessage(message("state", "状態: {state}", Map.of("state", state)));
    }

    private void help(CommandSender sender) {
        messages.messages("commands.overall.help", List.of(
                "/beat overall calculate - 総合順位を再計算",
                "/beat overall confirm - 総合結果を確定",
                "/beat overall result - 総合結果を表示")).forEach(sender::sendMessage);
    }

    private String message(String key, String fallback) { return messages.message("commands.overall." + key, fallback); }
    private String message(String key, String fallback, Map<String, ?> values) { return messages.message("commands.overall." + key, fallback, values); }
}
