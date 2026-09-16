package dev.konqasasas.beat.command;

import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.spigot.EmergencyOperationsService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class EmergencyCommand {
    private static final List<String> ACTIONS = List.of("cancel", "collect", "force-end", "restart");
    private final EmergencyOperationsService operations;
    private final ConfigurationFiles messages;
    private final Map<String, Pending> confirmations = new HashMap<>();

    public EmergencyCommand(EmergencyOperationsService operations, ConfigurationFiles messages) {
        this.operations = operations;
        this.messages = messages;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1 || !ACTIONS.contains(args[0].toLowerCase(Locale.ROOT))) { help(sender); return true; }
        String action = args[0].toLowerCase(Locale.ROOT);
        String key = sender instanceof Player player ? player.getUniqueId().toString() : "console";
        long now = System.currentTimeMillis();
        boolean confirmed = args.length == 2 && args[1].equalsIgnoreCase("confirm");
        Pending pending = confirmations.get(key);
        if (!confirmed || pending == null || !pending.action().equals(action) || pending.expiresAt() < now) {
            confirmations.put(key, new Pending(action, now + messages.configLong(
                    "confirmation-timeouts.dangerous-action-millis", 30_000L, 1_000L, 300_000L)));
            sender.sendMessage(message("confirm", "[BEAT] 緊急操作 '{action}' を実行します。30秒以内に同じコマンドへ confirm を付けて再実行してください。", Map.of("action", action)));
            return true;
        }
        confirmations.remove(key);
        try {
            switch (action) {
                case "cancel" -> { operations.cancelPhase(); sender.sendMessage(message("cancelled", "[BEAT] 現在フェーズを安全状態へ中止しました。")); }
                case "collect" -> sender.sendMessage(message("collected", "[BEAT] オンライン参加者 {count} 人を回収しました。", Map.of("count", operations.collectParticipants())));
                case "force-end" -> { operations.forceEnd(); sender.sendMessage(message("forced", "[BEAT] 現在の競技を強制終了し、結果を確定しました。")); }
                case "restart" -> sender.sendMessage(message("restarted", "[BEAT] 現在の競技を再試合準備へ戻しました: {state}", Map.of("state", operations.restartCurrent())));
                default -> throw new IllegalStateException("unknown emergency action");
            }
        } catch (PersistenceException | IllegalStateException exception) {
            sender.sendMessage(messages.message("commands.error", "[BEAT] ERROR: {message}", Map.of("message", String.valueOf(exception.getMessage()))));
        }
        return true;
    }

    public List<String> tab(String[] args) {
        if (args.length == 1) return matching(ACTIONS, args[0]);
        if (args.length == 2 && ACTIONS.contains(args[0].toLowerCase(Locale.ROOT))) return matching(List.of("confirm"), args[1]);
        return List.of();
    }

    private void help(CommandSender sender) {
        messages.messages("commands.emergency.help", List.of("{primary}--- BEAT 緊急操作 ---{reset}", "/beat emergency <cancel|collect|force-end|restart> [confirm]")).forEach(sender::sendMessage);
    }
    private String message(String key, String fallback) { return messages.message("commands.emergency." + key, fallback); }
    private String message(String key, String fallback, Map<String, ?> values) { return messages.message("commands.emergency." + key, fallback, values); }
    private static List<String> matching(List<String> values, String input) { String prefix = input.toLowerCase(Locale.ROOT); return values.stream().filter(value -> value.startsWith(prefix)).toList(); }
    private record Pending(String action, long expiresAt) {}
}
