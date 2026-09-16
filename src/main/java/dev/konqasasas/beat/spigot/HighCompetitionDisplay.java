package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.domain.high.HighCompetitionSession;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;

public final class HighCompetitionDisplay {
    private final ConfigurationFiles configuration;
    private final BossBar bossBar;
    private final ActionBarFeedbackState feedback = new ActionBarFeedbackState();

    public HighCompetitionDisplay(ConfigurationFiles configuration) {
        this.configuration = configuration;
        this.bossBar = Bukkit.createBossBar(
                configuration.message("ui.bossbar.high-title", "HIGH DIFFICULTY"),
                configuration.barColor("boss-bars.high", BarColor.BLUE),
                configuration.barStyle("boss-bars.high", BarStyle.SOLID));
    }

    public void add(Player player) { bossBar.addPlayer(player); }

    public void update(long tick, long totalTicks, Long nextEliminationTick, Integer requiredCourse,
            HighCompetitionSession session) {
        bossBar.setProgress(Math.max(0D, Math.min(1D, (double) (totalTicks - tick) / totalTicks)));
        String next = nextEliminationTick == null ? "--:--" : format(nextEliminationTick - tick);
        String title = configuration.message(
                "ui.bossbar.competition-time",
                "残り時間 {remaining} ｜ 次の脱落 {next}",
                Map.of("remaining", format(totalTicks - tick), "next", next));
        if (requiredCourse != null) title += " (Course " + requiredCourse + "未到達)";
        bossBar.setTitle(title);
        if (tick % configuration.configInt("ui-update-ticks.action-bar", 10, 1, 1200) == 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!session.contains(player.getUniqueId())) continue;
                String feedbackMessage = feedback.activeMessage(player.getUniqueId(), tick);
                if (feedbackMessage != null) {
                    actionBar(player, feedbackMessage);
                } else {
                    actionBar(player, configuration.message(
                            "ui.high.status",
                            "Point {points} ｜ #{rank} ｜ Course {course}",
                            Map.of(
                                    "points", "%03d".formatted(session.record(player.getUniqueId()).points()),
                                    "rank", "%02d".formatted(session.rank(player.getUniqueId())),
                                    "course", "%02d".formatted(session.currentCourse(player.getUniqueId())))));
                }
            }
        }
    }

    public void feedback(Player player, String message, long untilTick) {
        String notification = configuration.actionBarNotification(message);
        feedback.show(player.getUniqueId(), notification, untilTick);
        actionBar(player, notification);
    }

    public void updateRanking(HighCompetitionSession session) {
        var manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        var rankings = session.rankings();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!session.contains(viewer.getUniqueId())) continue;
            var scoreboard = manager.getNewScoreboard();
            var objective = scoreboard.registerNewObjective("beat_high", Criteria.DUMMY,
                    configuration.message("ui.scoreboard.high-title", "HIGH DIFFICULTY"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            int score = Math.min(10, rankings.size());
            for (var entry : rankings.stream().limit(10).toList()) {
                String line = configuration.message("ui.scoreboard.high-row", "#{rank} {points}pt {player}", Map.of(
                        "rank", "%02d".formatted(entry.rank()), "points", "%03d".formatted(entry.record().points()),
                        "player", trim(entry.competitor().tournamentName(), 16)));
                objective.getScore(line).setScore(score--);
            }
            viewer.setScoreboard(scoreboard);
        }
        for (var entry : rankings) {
            Player player = Bukkit.getPlayer(entry.competitor().uuid());
            if (player != null) { player.setPlayerListOrder(entry.rank()); player.setPlayerListName(
                    configuration.message("ui.tab.high-row", "#{rank} {points}pt {player}", Map.of(
                            "rank", "%02d".formatted(entry.rank()), "points", "%03d".formatted(entry.record().points()),
                            "player", entry.competitor().tournamentName()))); }
        }
    }

    public void clear(HighCompetitionSession session) {
        bossBar.removeAll();
        feedback.clear();
        var manager = Bukkit.getScoreboardManager();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!session.contains(player.getUniqueId())) continue;
            player.setPlayerListName(player.getName());
            player.setPlayerListOrder(0);
            if (manager != null) player.setScoreboard(manager.getMainScoreboard());
            actionBar(player, "");
        }
    }

    private static void actionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }
    private static String format(long ticks) {
        long seconds = Math.max(0, (ticks + 19) / 20);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }
    private static String trim(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
}
