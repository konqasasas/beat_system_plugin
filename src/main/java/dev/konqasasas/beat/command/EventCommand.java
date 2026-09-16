package dev.konqasasas.beat.command;

import dev.konqasasas.beat.application.EventResetService;
import dev.konqasasas.beat.application.ResultEditingService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.debug.DebugService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.spigot.EmergencyOperationsService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EventCommand {
    private final EventResetService resets;
    private final EmergencyOperationsService operations;
    private final DebugService debug;
    private final ResultEditingService resultEditor;
    private final ConfigurationFiles configuration;
    private final Map<String, Long> confirmations = new HashMap<>();

    public EventCommand(EventResetService resets, EmergencyOperationsService operations,
            DebugService debug, ResultEditingService resultEditor, ConfigurationFiles configuration) {
        this.resets = resets;
        this.operations = operations;
        this.debug = debug;
        this.resultEditor = resultEditor;
        this.configuration = configuration;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("reset")) {
            help(sender);
            return true;
        }
        String key = sender instanceof Player player ? player.getUniqueId().toString() : "console";
        long now = System.currentTimeMillis();
        boolean confirmed = args.length == 2 && args[1].equalsIgnoreCase("confirm")
                && confirmations.getOrDefault(key, 0L) >= now;
        if (!confirmed) {
            long timeoutMillis = configuration.configLong(
                    "confirmation-timeouts.dangerous-action-millis", 30_000L, 1_000L, 300_000L);
            confirmations.put(key, now + timeoutMillis);
            sender.sendMessage(configuration.message(
                    "commands.event.reset-confirm",
                    "[BEAT] 大会データを初期化します。{seconds}秒以内に /beat event reset confirm を実行してください。",
                    Map.of("seconds", Math.max(1L, (timeoutMillis + 999L) / 1000L))));
            return true;
        }

        confirmations.remove(key);
        try {
            EventResetService.ResetResult result = resets.reset(() -> {
                operations.shutdownForEventReset();
                debug.disable();
            });
            resultEditor.clearUndoHistory();
            sender.sendMessage(configuration.message(
                    "commands.event.reset-completed",
                    "[BEAT] 大会データを初期化しました。バックアップ: {count}件",
                    Map.of("count", result.backups().size())));
        } catch (PersistenceException | RuntimeException exception) {
            sender.sendMessage(configuration.message(
                    "commands.event.reset-failed",
                    "[BEAT] ERROR: 大会データの初期化に失敗しました: {message}",
                    Map.of("message", String.valueOf(exception.getMessage()))));
        }
        return true;
    }

    public List<String> tab(String[] args) {
        if (args.length == 1) return matching(List.of("reset"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return matching(List.of("confirm"), args[1]);
        }
        return List.of();
    }

    private void help(CommandSender sender) {
        configuration.messages("commands.event.help", List.of(
                "{primary}--- BEAT 大会データ操作 ---{reset}",
                "/beat event reset - 大会データをバックアップ後に初期化（二段階確認）"))
                .forEach(sender::sendMessage);
    }

    private static List<String> matching(List<String> values, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
