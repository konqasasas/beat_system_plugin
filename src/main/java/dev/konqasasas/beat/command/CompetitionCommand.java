package dev.konqasasas.beat.command;

import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.application.HighResultService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.spigot.HighPracticeController;
import dev.konqasasas.beat.spigot.HighCompetitionController;
import dev.konqasasas.beat.spigot.TimeAttackController;
import dev.konqasasas.beat.spigot.EnduranceController;
import java.util.List;

public final class CompetitionCommand {
    private final HighPracticeController highPractice;
    private final EventStateService eventState;
    private final HighCompetitionController highCompetition;
    private final HighResultService highResults;
    private final TimeAttackController timeAttack;
    private EnduranceController endurance;
    private final ConfigurationFiles messages;
    private final java.util.Map<String, Long> restartConfirmations = new java.util.HashMap<>();

    public CompetitionCommand(HighPracticeController hp, HighCompetitionController hc,
            TimeAttackController ta, EnduranceController end, EventStateService state,
            HighResultService results, ConfigurationFiles messages) {
        highPractice=hp; highCompetition=hc; timeAttack=ta; endurance=end;
        eventState=state; highResults=results; this.messages=messages;
    }

    public boolean execute(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(message("status", "[BEAT] 大会状態: {state}", java.util.Map.of("state", eventState.current().tournamentState())));
            return true;
        }
        if (args.length == 2 && ((args[0].equalsIgnoreCase("ta") && args[1].equalsIgnoreCase("start"))
                || (args[0].equalsIgnoreCase("start") && args[1].equalsIgnoreCase("ta")))) {
            try { timeAttack.start(); sender.sendMessage(message("ta-started", "[BEAT] TA開始カウントダウンを開始しました。")); }
            catch (PersistenceException | IllegalStateException exception) { sender.sendMessage(error(exception)); }
            return true;
        }
        if (args.length==2&&((args[0].equalsIgnoreCase("endurance")&&args[1].equalsIgnoreCase("start"))||(args[0].equalsIgnoreCase("start")&&args[1].equalsIgnoreCase("endurance")))){try{endurance.start();sender.sendMessage(message("endurance-started","[BEAT] 耐久開始カウントダウンを開始しました。"));}catch(PersistenceException|IllegalStateException exception){sender.sendMessage(error(exception));}return true;}
        boolean highStart = args.length == 2
                && ((args[0].equalsIgnoreCase("high") && args[1].equalsIgnoreCase("start"))
                        || (args[0].equalsIgnoreCase("start") && args[1].equalsIgnoreCase("high")));
        boolean highRestart = args.length >= 2
                && args[0].equalsIgnoreCase("restart") && args[1].equalsIgnoreCase("high");
        if (highRestart) {
            String key = sender instanceof org.bukkit.entity.Player player
                    ? player.getUniqueId().toString() : "console";
            long now = System.currentTimeMillis();
            boolean confirmed = args.length == 3 && args[2].equalsIgnoreCase("confirm")
                    && restartConfirmations.getOrDefault(key, 0L) >= now;
            if (!confirmed) {
                restartConfirmations.put(key, now + messages.configLong(
                        "confirmation-timeouts.dangerous-action-millis", 30_000L, 1_000L, 300_000L));
                sender.sendMessage(message("restart-confirm", "[BEAT] 高難易度の現在記録を破棄します。30秒以内に /beat competition restart high confirm を実行してください。"));
                return true;
            }
            restartConfirmations.remove(key);
            try {
                TournamentState current = eventState.current().tournamentState();
                if (switch (current) {
                    case WAITING, HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE, HIGH_PREPARE,
                            HIGH_RUNNING, HIGH_FINISHED -> false;
                    default -> true;
                }) throw new IllegalStateException("現在は高難易度を再試合できる状態ではありません: " + current);
                boolean repeatPractice = current == TournamentState.HIGH_PRACTICE_COUNTDOWN
                        || current == TournamentState.HIGH_PRACTICE || current == TournamentState.WAITING;
                highPractice.shutdown();
                highCompetition.shutdown();
                highCompetition.resetRegisteredPlayers();
                highResults.clearHigh();
                eventState.resetHighForRestart(repeatPractice);
                sender.sendMessage(message(repeatPractice ? "restart-practice" : "restart-running",
                        repeatPractice ? "[BEAT] 高難易度を練習前へ戻しました。自動開始はしません。"
                                : "[BEAT] 高難易度を本番開始待ちへ戻しました。自動開始はしません。"));
            } catch (PersistenceException | IllegalStateException exception) {
                sender.sendMessage(error(exception));
            }
            return true;
        }
        if (!highStart) {
            help(sender);
            return true;
        }
        try {
            if (eventState.current().tournamentState() == TournamentState.HIGH_PREPARE) {
                highCompetition.startFromPrepare();
                sender.sendMessage(message("high-running-started", "[BEAT] 高難易度本番を開始しました。"));
            } else {
                highPractice.start();
                sender.sendMessage(message("high-practice-started", "[BEAT] 高難易度の練習開始カウントダウンを開始しました。"));
            }
        } catch (PersistenceException | IllegalStateException exception) {
            sender.sendMessage(error(exception));
        }
        return true;
    }

    public List<String> tab(String[] args) {
        if (args.length == 1) return matching(List.of("high", "ta", "endurance", "start", "restart", "status"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("high")) return matching(List.of("start"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) return matching(List.of("high","ta","endurance"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("ta")) return matching(List.of("start"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("endurance")) return matching(List.of("start"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("restart")) return matching(List.of("high"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("restart") && args[1].equalsIgnoreCase("high")) return matching(List.of("confirm"), args[2]);
        return List.of();
    }

    public void help(org.bukkit.command.CommandSender sender) {
        messages.messages("commands.competition.help", List.of(
                "{primary}--- BEAT 競技操作 ---{reset}",
                "/beat competition high start - 高難易度の練習から開始",
                "/beat competition status - 現在の大会状態",
                "/beat competition ta start - タイムアタックを開始",
                "/beat competition endurance start - 耐久を開始",
                "/beat competition restart high - 高難易度を二段階確認で再試合準備へ戻す")).forEach(sender::sendMessage);
    }

    private String message(String key, String fallback) { return messages.message("commands.competition." + key, fallback); }
    private String message(String key, String fallback, java.util.Map<String, ?> values) { return messages.message("commands.competition." + key, fallback, values); }
    private String error(Exception exception) { return messages.message("commands.error", "[BEAT] ERROR: {message}", java.util.Map.of("message", String.valueOf(exception.getMessage()))); }

    private static List<String> matching(List<String> candidates, String input) {
        String prefix = input.toLowerCase(java.util.Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)).toList();
    }
}
