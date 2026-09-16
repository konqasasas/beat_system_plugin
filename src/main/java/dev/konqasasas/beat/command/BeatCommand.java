package dev.konqasasas.beat.command;

import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.application.WhitelistService;
import dev.konqasasas.beat.application.WhitelistStatus;
import dev.konqasasas.beat.application.WhitelistSyncResult;
import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.configuration.ConfigurationLoadException;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RegisteredIdentity;
import dev.konqasasas.beat.roster.RosterException;
import dev.konqasasas.beat.roster.RosterService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class BeatCommand implements TabExecutor {
    private static final List<String> ROOT_COMMANDS =
            List.of("menu", "players", "player", "whitelist", "competition", "settings", "result", "overall", "setup", "event", "emergency", "debug", "reload", "help");

    private final RosterService rosters;
    private final EventStateService eventState;
    private final WhitelistService whitelists;
    private final AdminAuthorizer admins;
    private final SetupCommand setup;
    private final CompetitionCommand competitions;
    private final ResultCommand results;
    private final OverallCommand overall;
    private final EmergencyCommand emergency;
    private final EventCommand event;
    private final DebugCommand debug;
    private final CompetitionSettingsCommand settings;
    private final dev.konqasasas.beat.application.OverallService overallService;
    private final dev.konqasasas.beat.gui.BeatAdminMenu menu;
    private final ConfigurationFiles configurationFiles;
    private final CompetitionSettingsService competitionSettings;
    private final Logger logger;

    public BeatCommand(
            RosterService rosters,
            EventStateService eventState,
            WhitelistService whitelists,
            AdminAuthorizer admins,
            SetupCommand setup,
            CompetitionCommand competitions,
            ResultCommand results, OverallCommand overall, EventCommand event,
            EmergencyCommand emergency, DebugCommand debug, CompetitionSettingsCommand settings,
            dev.konqasasas.beat.application.OverallService overallService,
            dev.konqasasas.beat.gui.BeatAdminMenu menu,
            ConfigurationFiles configurationFiles, CompetitionSettingsService competitionSettings,
            Logger logger) {
        this.rosters = rosters;
        this.eventState = eventState;
        this.whitelists = whitelists;
        this.admins = admins;
        this.setup = setup;
        this.competitions = competitions;
        this.results = results;
        this.overall = overall;
        this.event = event;
        this.emergency = emergency;
        this.debug = debug;
        this.settings = settings;
        this.overallService = overallService;
        this.menu = menu;
        this.configurationFiles = configurationFiles;
        this.competitionSettings = competitionSettings;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(
            CommandSender sender, Command command, String label, String[] args) {
        if (!authorized(sender)) {
            sender.sendMessage(configurationFiles.message(
                    "commands.no-permission", "[BEAT] このコマンドを使用する権限がありません。"));
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) menu.open(player); else sendHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "players" -> players(sender);
            case "player" -> playerInfo(sender, args);
            case "whitelist" -> whitelist(sender, args);
            case "setup" -> setup.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "competition" -> competitions.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "result" -> results.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "overall" -> overall.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "event" -> event.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "emergency" -> emergency.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "debug" -> debug.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "settings" -> settings.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "menu" -> { if (sender instanceof Player player) menu.open(player); else sender.sendMessage(configurationFiles.message("commands.player-only", "[BEAT] menuはゲーム内専用です。")); yield true; }
            default -> {
                sender.sendMessage(configurationFiles.message(
                        "commands.unknown", "[BEAT] 不明なサブコマンドです。/beat help を確認してください。"));
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String label, String[] args) {
        if (!authorized(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return matching(ROOT_COMMANDS, args[0]);
        }
        if (args[0].equalsIgnoreCase("setup")) {
            return setup.tab(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        }
        if (args[0].equalsIgnoreCase("competition")) {
            return competitions.tab(java.util.Arrays.copyOfRange(args, 1, args.length));
        }
        if (args[0].equalsIgnoreCase("result")) {
            return results.tab(java.util.Arrays.copyOfRange(args, 1, args.length));
        }
        if (args[0].equalsIgnoreCase("overall")) return overall.tab(java.util.Arrays.copyOfRange(args,1,args.length));
        if (args[0].equalsIgnoreCase("event")) return event.tab(java.util.Arrays.copyOfRange(args,1,args.length));
        if (args[0].equalsIgnoreCase("emergency")) return emergency.tab(java.util.Arrays.copyOfRange(args,1,args.length));
        if (args[0].equalsIgnoreCase("debug")) return debug.tab(java.util.Arrays.copyOfRange(args,1,args.length));
        if (args[0].equalsIgnoreCase("settings")) return settings.tab(java.util.Arrays.copyOfRange(args,1,args.length));
        if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
            return matching(List.of("admins", "all", "status"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return matching(List.of("info","overall-exclude","overall-include","disqualify","undisqualify"), args[1]);
        }
        if (args.length == 3
                && args[0].equalsIgnoreCase("player")
                && List.of("info","overall-exclude","overall-include","disqualify","undisqualify").contains(args[1].toLowerCase(Locale.ROOT))) {
            return matching(
                    rosters.current().participants().values().stream()
                            .map(RegisteredIdentity::mcid)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList(),
                    args[2]);
        }
        return List.of();
    }

    private boolean reload(CommandSender sender) {
        if (switch (eventState.current().tournamentState()) {
            case HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE, HIGH_PREPARE, HIGH_RUNNING,
                    TA_COUNTDOWN, TA_RUNNING, ENDURANCE_COUNTDOWN, ENDURANCE_RUNNING -> true;
            default -> false;
        }) {
            sender.sendMessage(configurationFiles.message(
                    "reload.blocked", "[BEAT] 競技進行中のためreloadできません。"));
            return true;
        }
        try {
            configurationFiles.reload();
            competitionSettings.reload();
            setup.reload();
            rosters.reload();
            sender.sendMessage(configurationFiles.message(
                    "reload.success",
                    "[BEAT] config・competition-settings・messages・styles・JSON・マップ設定を再読み込みしました。"));
        } catch (RosterException | PersistenceException | ConfigurationLoadException exception) {
            logger.log(Level.WARNING, "BEAT configuration reload failed; keeping the last known good state.", exception);
            sender.sendMessage(configurationFiles.message(
                    "reload.failed",
                    "[BEAT] ERROR: 再読み込みに失敗しました。以前の正常な設定を維持します。"));
        }
        return true;
    }

    private boolean players(CommandSender sender) {
        sender.sendMessage(configurationFiles.message("commands.players.header", "{primary}--- BEAT 参加者 ---{reset}"));
        rosters.current().participants().values().stream()
                .sorted((left, right) -> left.mcid().compareToIgnoreCase(right.mcid()))
                .forEach(identity -> sender.sendMessage(configurationFiles.message(
                        "commands.players.row", "{mcid} ({uuid})",
                        Map.of("mcid", identity.mcid(), "uuid", identity.uuid()))));
        sender.sendMessage(configurationFiles.message(
                "commands.players.total", "合計: {count}",
                Map.of("count", rosters.current().participants().size())));
        return true;
    }

    private boolean playerInfo(CommandSender sender, String[] args) {
        if (args.length == 3 && !args[1].equalsIgnoreCase("info")) {
            try {
                switch (args[1].toLowerCase(Locale.ROOT)) {
                    case "overall-exclude" -> overallService.setPlayerFlag(args[2], true, null);
                    case "overall-include" -> overallService.setPlayerFlag(args[2], false, null);
                    case "disqualify" -> {
                        overallService.setPlayerFlag(args[2], true, true);
                        org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(args[2]);
                        if (target != null) target.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    }
                    case "undisqualify" -> overallService.setPlayerFlag(args[2], null, false);
                    default -> { sender.sendMessage(configurationFiles.message("commands.player.usage", "使用例: /beat player info <player>")); return true; }
                }
                sender.sendMessage(configurationFiles.message(
                        "commands.player.updated", "[BEAT] プレイヤー設定を更新しました: {player}",
                        Map.of("player", args[2])));
            } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
                sender.sendMessage(error(exception));
            }
            return true;
        }
        if (args.length != 3 || !args[1].equalsIgnoreCase("info")) {
            sender.sendMessage(configurationFiles.message("commands.player.usage", "使用例: /beat player info <player>"));
            return true;
        }
        RegisteredIdentity identity = rosters.current().findParticipant(args[2]).orElse(null);
        if (identity == null) {
            sender.sendMessage(configurationFiles.message(
                    "commands.player.not-found", "[BEAT] 参加者が見つかりません: {player}",
                    Map.of("player", args[2])));
            return true;
        }
        sender.sendMessage(configurationFiles.message("commands.player.header", "{primary}--- BEAT Player Info ---{reset}"));
        sender.sendMessage(configurationFiles.message(
                "commands.player.mcid", "登録MCID: {mcid}", Map.of("mcid", identity.mcid())));
        sender.sendMessage(configurationFiles.message(
                "commands.player.uuid", "UUID: {uuid}", Map.of("uuid", identity.uuid())));
        sender.sendMessage(configurationFiles.message(
                "commands.player.tournament-name", "大会時MCID: {name}",
                Map.of("name", eventState.tournamentName(identity.uuid()).orElse(
                        configurationFiles.message("commands.player.not-participated", "未参加")))));
        return true;
    }

    private boolean whitelist(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.usage", "使用例: /beat whitelist <admins|all|status>"));
            return true;
        }
        if (args[1].equalsIgnoreCase("status")) {
            WhitelistStatus status = whitelists.status();
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.mode", "Whitelist mode: {mode}", Map.of("mode", status.mode())));
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.counts", "Configured: {configured} / Actual: {actual}",
                    Map.of("configured", status.configuredPlayers(), "actual", status.whitelistedPlayers())));
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.synchronized", "完全同期: {value}",
                    Map.of("value", configurationFiles.message(
                            status.synchronizedExactly()
                                    ? "commands.whitelist.synchronized-yes"
                                    : "commands.whitelist.synchronized-no",
                            status.synchronizedExactly() ? "YES" : "NO"))));
            return true;
        }
        WhitelistMode mode;
        if (args[1].equalsIgnoreCase("admins")) {
            mode = WhitelistMode.ADMIN_ONLY;
        } else if (args[1].equalsIgnoreCase("all")) {
            mode = WhitelistMode.ALL;
        } else {
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.usage", "使用例: /beat whitelist <admins|all|status>"));
            return true;
        }
        try {
            WhitelistSyncResult result = whitelists.synchronize(mode);
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.completed", "[BEAT] Whitelistを完全同期しました: {mode}",
                    Map.of("mode", result.mode())));
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.changes", "対象: {configured} / 追加: {added} / 削除: {removed}",
                    Map.of(
                            "configured", result.configuredPlayers(),
                            "added", result.added(),
                            "removed", result.removed())));
        } catch (PersistenceException exception) {
            sender.sendMessage(configurationFiles.message(
                    "commands.whitelist.failed", "[BEAT] ERROR: Whitelistモードを保存できませんでした。"));
        }
        return true;
    }

    private boolean authorized(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return true;
        }
        return sender instanceof Player player && admins.isAdmin(player.getUniqueId());
    }

    private static List<String> matching(List<String> candidates, String input) {
        String prefix = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private void sendHelp(CommandSender sender) {
        configurationFiles.messages("commands.help", List.of(
                "{primary}--- BEAT 管理コマンド ---{reset}",
                "/beat players - 登録参加者一覧",
                "/beat player info <player> - 参加者情報",
                "/beat whitelist admins - 運営のみへ完全同期",
                "/beat whitelist all - 運営と参加者へ完全同期",
                "/beat whitelist status - 現在の同期状態",
                "/beat reload - 全設定・参加者・運営JSONを再読込",
                "/beat setup help - マップセットアップ",
                "/beat competition high start - 高難易度を開始",
                "/beat result high - 高難易度の確定結果を表示",
                "/beat overall <calculate|confirm|result> - 総合順位",
                "/beat event reset - 大会データをバックアップ後に初期化",
                "/beat emergency <cancel|collect|force-end|restart> - 緊急操作（二段階確認）",
                "/beat debug <enable|scenario|...> - 分離デバッグ環境")).forEach(sender::sendMessage);
    }

    private String error(Exception exception) {
        return configurationFiles.message(
                "commands.error", "[BEAT] ERROR: {message}",
                Map.of("message", String.valueOf(exception.getMessage())));
    }
}
