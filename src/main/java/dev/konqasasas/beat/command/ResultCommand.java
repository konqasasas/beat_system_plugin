package dev.konqasasas.beat.command;

import dev.konqasasas.beat.application.EnduranceResultService;
import dev.konqasasas.beat.application.HighResultService;
import dev.konqasasas.beat.application.ResultEditingService;
import dev.konqasasas.beat.application.TimeAttackResultService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class ResultCommand {
    private final HighResultService results;
    private final TimeAttackResultService timeAttack;
    private final EnduranceResultService endurance;
    private final ResultEditingService editor;
    private final ConfigurationFiles messages;

    public ResultCommand(HighResultService results, TimeAttackResultService timeAttack,
            EnduranceResultService endurance, ResultEditingService editor, ConfigurationFiles messages) {
        this.results = results;
        this.timeAttack = timeAttack;
        this.endurance = endurance;
        this.editor = editor;
        this.messages = messages;
    }

    public boolean execute(CommandSender sender, String[] args) {
        try {
            if (args.length == 1 && args[0].equalsIgnoreCase("undo")) {
                editor.undo(sender.getName());
                sender.sendMessage(message("undo", "[BEAT] 直前の結果編集を戻しました。"));
                return true;
            }
            if (args.length >= 2 && edit(sender, args)) return true;
        } catch (PersistenceException | IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage(error(exception));
            return true;
        }
        if (args.length != 1 || !List.of("high", "ta", "endurance").contains(args[0].toLowerCase(java.util.Locale.ROOT))) {
            sender.sendMessage(message("usage", "使用例: /beat result <high|ta|endurance>"));
            return true;
        }
        try {
            showConfirmed(sender, args[0].toLowerCase(java.util.Locale.ROOT));
        } catch (PersistenceException exception) {
            sender.sendMessage(message("load-failed", "[BEAT] ERROR: 結果を読み込めませんでした。"));
        }
        return true;
    }

    private boolean edit(CommandSender sender, String[] args) throws PersistenceException {
        ResultEditingService.Competition competition = parseCompetition(args[0]);
        if (args[1].equalsIgnoreCase("recalculate") && args.length == 2) {
            editor.recalculate(competition); sender.sendMessage(message("recalculated", "[BEAT] 順位を再計算しました。")); return true;
        }
        if (args[1].equalsIgnoreCase("confirm") && args.length == 2) {
            editor.confirm(sender.getName(), competition); sender.sendMessage(message("confirmed", "[BEAT] 競技結果を再確定しました。")); return true;
        }
        if (args[1].equalsIgnoreCase("show") && args.length == 2) { showRaw(sender, competition); return true; }
        if (!args[1].equalsIgnoreCase("edit")) return false;
        if (competition == ResultEditingService.Competition.TA && args.length == 6 && args[3].equalsIgnoreCase("split")) {
            editor.edit(sender.getName(), competition, args[2], "split", args[5], Integer.parseInt(args[4]));
            sender.sendMessage(message("edited", "[BEAT] 編集しました。recalculateを実行してください。")); return true;
        }
        if (args.length == 5) {
            editor.edit(sender.getName(), competition, args[2], args[3].toLowerCase(java.util.Locale.ROOT), args[4], null);
            sender.sendMessage(message("edited", "[BEAT] 編集しました。recalculateを実行してください。")); return true;
        }
        return false;
    }

    private void showConfirmed(CommandSender sender, String kind) throws PersistenceException {
        var snapshot = switch (kind) { case "ta" -> timeAttack.current(); case "endurance" -> endurance.current(); default -> results.current(); };
        if (kind.equals("endurance")) {
            if (!snapshot.enduranceConfirmed()) { sender.sendMessage(message("not-confirmed-endurance", "[BEAT] 耐久結果は未確定です。")); return; }
            sender.sendMessage(message("header-endurance", "{primary}--- ENDURANCE RESULT ---{reset}"));
            snapshot.players().stream().filter(player -> player.endurance() != null)
                    .sorted(Comparator.comparingInt(player -> player.endurance().rank()))
                    .forEach(player -> sender.sendMessage(rowEndurance(player.endurance().rank(), player.endurance().maxProgress(), player.tournamentName())));
            return;
        }
        if (kind.equals("ta")) {
            if (!snapshot.timeAttackConfirmed()) { sender.sendMessage(message("not-confirmed-ta", "[BEAT] TAの結果は未確定です。")); return; }
            sender.sendMessage(message("header-ta", "{primary}--- TIME ATTACK RESULT ---{reset}"));
            snapshot.players().stream().filter(player -> player.timeAttack() != null)
                    .sorted(Comparator.comparingInt(player -> player.timeAttack().rank()))
                    .forEach(player -> sender.sendMessage(rowTa(player.timeAttack().rank(), player.timeAttack().pbTicks(), player.tournamentName())));
            return;
        }
        if (!snapshot.highConfirmed()) { sender.sendMessage(message("not-confirmed-high", "[BEAT] 高難易度の結果はまだ確定していません。")); return; }
        sender.sendMessage(message("header-high", "{primary}--- HIGH DIFFICULTY RESULT ---{reset}"));
        snapshot.players().stream().filter(player -> player.high() != null && player.high().rank() != null)
                .sorted(Comparator.comparingInt(player -> player.high().rank()))
                .forEach(player -> sender.sendMessage(rowHigh(player.high().rank(), player.high().points(), player.tournamentName())));
    }

    public List<String> tab(String[] args) {
        if (args.length == 1) return List.of("high", "ta", "endurance", "undo").stream().filter(value -> value.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))).toList();
        if (args.length == 2) return List.of("edit", "recalculate", "show", "confirm").stream().filter(value -> value.startsWith(args[1].toLowerCase(java.util.Locale.ROOT))).toList();
        return List.of();
    }

    private void showRaw(CommandSender sender, ResultEditingService.Competition competition) throws PersistenceException {
        var result = editor.current();
        boolean confirmed = switch (competition) { case HIGH -> result.highConfirmed(); case TA -> result.timeAttackConfirmed(); case ENDURANCE -> result.enduranceConfirmed(); };
        sender.sendMessage(message("header-raw", "{primary}--- {competition} RESULT ({state}) ---{reset}", Map.of(
                "competition", competition, "state", confirmed ? "確定" : "未確定")));
        for (var player : result.players()) {
            switch (competition) {
                case HIGH -> { if (player.high() != null) sender.sendMessage(rowHigh(player.high().rank(), player.high().points(), player.tournamentName())); }
                case TA -> { if (player.timeAttack() != null) sender.sendMessage(rowTa(player.timeAttack().rank(), player.timeAttack().pbTicks(), player.tournamentName())); }
                case ENDURANCE -> { if (player.endurance() != null) sender.sendMessage(rowEndurance(player.endurance().rank(), player.endurance().maxProgress(), player.tournamentName())); }
            }
        }
    }

    private String rowHigh(Integer rank, int points, String player) { return message("row-high", "#{rank} {points}pt {player}", Map.of("rank", rank(rank), "points", "%03d".formatted(points), "player", player)); }
    private String rowTa(Integer rank, Long ticks, String player) { return message("row-ta", "#{rank} {time} {player}", Map.of("rank", rank(rank), "time", ticks == null ? "--.--" : format(ticks), "player", player)); }
    private String rowEndurance(Integer rank, int progress, String player) { return message("row-endurance", "#{rank} Progress {progress} {player}", Map.of("rank", rank(rank), "progress", "%03d".formatted(progress), "player", player)); }
    private String message(String key, String fallback) { return messages.message("commands.result." + key, fallback); }
    private String message(String key, String fallback, Map<String, ?> values) { return messages.message("commands.result." + key, fallback, values); }
    private String error(Exception exception) { return messages.message("commands.error", "[BEAT] ERROR: {message}", Map.of("message", String.valueOf(exception.getMessage()))); }
    private static String rank(Integer rank) { return rank == null ? "--" : "%02d".formatted(rank); }
    private static ResultEditingService.Competition parseCompetition(String value) { try { return ResultEditingService.Competition.valueOf(value.toUpperCase(java.util.Locale.ROOT)); } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("競技はhigh/ta/enduranceです"); } }
    private static String format(long ticks) { return "%02d.%02d".formatted(ticks / 20, (ticks % 20) * 5); }
}
