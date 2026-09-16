package dev.konqasasas.beat.command;

import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.configuration.CompetitionSettings;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.configuration.ConfigurationLoadException;
import dev.konqasasas.beat.domain.state.TournamentState;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandSender;

public final class CompetitionSettingsCommand {
    public static final List<String> KEYS = List.of(
            "start-countdown", "high-practice", "high-prepare", "high-total",
            "high-elimination-1", "high-elimination-2", "high-elimination-3", "high-elimination-4",
            "ta-total", "ta-elimination-1", "ta-elimination-2", "ta-survivors-1", "ta-survivors-2",
            "endurance-total", "endurance-elimination-1", "endurance-elimination-2");

    private final CompetitionSettingsService settings;
    private final EventStateService states;

    public CompetitionSettingsCommand(CompetitionSettingsService settings, EventStateService states) {
        this.settings = settings;
        this.states = states;
    }

    public boolean execute(CommandSender sender, String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("show")) {
                show(sender, settings.current());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("preset")) {
                ensureEditable();
                CompetitionSettings selected = switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "production" -> CompetitionSettings.production();
                    case "test" -> CompetitionSettings.testPreset();
                    default -> throw new IllegalArgumentException("presetはproductionまたはtestです");
                };
                settings.save(selected);
                sender.sendMessage("[BEAT] 競技設定プリセットを適用しました: " + args[1].toLowerCase(Locale.ROOT));
                show(sender, selected);
            } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
                ensureEditable();
                String key = args[1].toLowerCase(Locale.ROOT);
                if (!KEYS.contains(key)) throw new IllegalArgumentException("不明な設定キーです: " + key);
                long value = key.startsWith("ta-survivors-") ? integer(args[2]) : ticks(args[2]);
                CompetitionSettings updated = settings.current().with(key, value);
                settings.save(updated);
                sender.sendMessage("[BEAT] 競技設定を更新しました: " + key + " = " + display(key, value));
            } else {
                help(sender);
            }
        } catch (ConfigurationLoadException | IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage("[BEAT] ERROR: " + exception.getMessage());
        }
        return true;
    }

    public List<String> tab(String[] args) {
        if (args.length == 1) return matching(List.of("show", "set", "preset"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return matching(KEYS, args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("preset")) return matching(List.of("production", "test"), args[1]);
        return List.of();
    }

    public void ensureEditable() {
        if (active(states.current().tournamentState())) {
            throw new IllegalStateException("競技進行中は設定を変更できません");
        }
    }

    private static boolean active(TournamentState state) {
        return switch (state) {
            case HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE, HIGH_PREPARE, HIGH_RUNNING,
                    TA_COUNTDOWN, TA_RUNNING, ENDURANCE_COUNTDOWN, ENDURANCE_RUNNING -> true;
            default -> false;
        };
    }

    private static void show(CommandSender sender, CompetitionSettings value) {
        sender.sendMessage("--- BEAT 競技設定 ---");
        sender.sendMessage("開始カウントダウン: " + clock(value.startCountdownTicks()));
        sender.sendMessage("高難易度 練習 " + clock(value.highPracticeTicks()) + " / 準備 " + clock(value.highPrepareTicks())
                + " / 本番 " + clock(value.highRunningTicks()));
        sender.sendMessage("高難易度 脱落: " + clocks(value.highEliminationTicks()));
        sender.sendMessage("TA 本番 " + clock(value.timeAttackRunningTicks()) + " / 脱落 " + clocks(value.timeAttackEliminationTicks())
                + " / 生存人数 " + value.timeAttackSurvivorCounts());
        sender.sendMessage("耐久 本番 " + clock(value.enduranceRunningTicks()) + " / 脱落 " + clocks(value.enduranceEliminationTicks()));
    }

    private static void help(CommandSender sender) {
        sender.sendMessage("/beat settings show");
        sender.sendMessage("/beat settings set <key> <mm:ss|秒s|tick>  (TA生存人数のみ整数)");
        sender.sendMessage("/beat settings preset <production|test>");
    }

    private static long ticks(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        try {
            if (value.matches("\\d+:\\d{1,2}")) {
                String[] parts = value.split(":");
                int seconds = Integer.parseInt(parts[1]);
                if (seconds > 59) throw new IllegalArgumentException("秒は0から59です");
                return (Long.parseLong(parts[0]) * 60 + seconds) * 20;
            }
            if (value.endsWith("s")) return Long.parseLong(value.substring(0, value.length() - 1)) * 20;
            if (value.endsWith("t")) return Long.parseLong(value.substring(0, value.length() - 1));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("時間の形式が不正です: " + text);
        }
        throw new IllegalArgumentException("時間はmm:ss、10s、200tのいずれかで指定してください");
    }

    private static long integer(String text) {
        try { return Long.parseLong(text); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("人数は整数で指定してください"); }
    }

    private static String display(String key, long value) { return key.startsWith("ta-survivors-") ? Long.toString(value) : clock(value); }
    private static String clocks(List<Integer> values) { return values.stream().map(CompetitionSettingsCommand::clock).toList().toString(); }
    public static String clock(long ticks) { long seconds = Math.max(0, (ticks + 19) / 20); return "%02d:%02d".formatted(seconds / 60, seconds % 60); }
    private static List<String> matching(List<String> values, String input) { String prefix = input.toLowerCase(Locale.ROOT); return values.stream().filter(value -> value.startsWith(prefix)).toList(); }
}
