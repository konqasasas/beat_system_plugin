package dev.konqasasas.beat.spigot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/** One scoreboard shared by every viewer, updated only where rendered rows changed. */
final class SharedRankingBoard {
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final PlayerCollisionService collisions;
    private final Set<UUID> viewers = new HashSet<>();
    private Map<String, Integer> renderedScores = Map.of();

    SharedRankingBoard(String objectiveName, String title, PlayerCollisionService collisions) {
        var manager = Bukkit.getScoreboardManager();
        if (manager == null) throw new IllegalStateException("Scoreboard manager is unavailable");
        this.collisions = collisions;
        scoreboard = manager.getNewScoreboard();
        collisions.prepare(scoreboard);
        objective = scoreboard.registerNewObjective(objectiveName, Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    void show(Player player) {
        viewers.add(player.getUniqueId());
        collisions.show(player, scoreboard);
    }

    void update(List<String> rows) {
        List<String> visibleRows = rows.stream().limit(10).toList();
        Map<String, Integer> next = new LinkedHashMap<>();
        for (int index = 0; index < visibleRows.size(); index++) {
            next.put(visibleRows.get(index), visibleRows.size() - index);
        }
        renderedScores.forEach((line, score) -> {
            if (!score.equals(next.get(line))) scoreboard.resetScores(line);
        });
        next.forEach((line, score) -> {
            if (!score.equals(renderedScores.get(line))) objective.getScore(line).setScore(score);
        });
        renderedScores = Map.copyOf(next);
    }

    void clear() {
        renderedScores.keySet().forEach(scoreboard::resetScores);
        renderedScores = Map.of();
    }

    void hideAll() {
        var manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (UUID viewerId : Set.copyOf(viewers)) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null) collisions.show(viewer, manager.getMainScoreboard());
            }
        }
        viewers.clear();
    }
}
