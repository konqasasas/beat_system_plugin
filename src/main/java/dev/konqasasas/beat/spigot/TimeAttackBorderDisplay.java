package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ta.TimeAttackRecord;
import java.util.List;

final class TimeAttackBorderDisplay {
    private TimeAttackBorderDisplay() {}

    static String format(long tick, List<Integer> eliminationTicks, List<Integer> survivorCounts,
            List<RankedEntry<TimeAttackRecord>> ranking) {
        for (int index = 0; index < Math.min(eliminationTicks.size(), survivorCounts.size()); index++) {
            if (tick >= eliminationTicks.get(index)) continue;
            int targetRank = survivorCounts.get(index);
            String time = ranking.stream()
                    .filter(entry -> entry.rank() == targetRank && entry.record().hasPersonalBest())
                    .findFirst()
                    .map(entry -> formatTicks(entry.record().personalBestTicks().orElseThrow()))
                    .orElse("--.--");
            return "#%02d %s".formatted(targetRank, time);
        }
        return "--.--";
    }

    private static String formatTicks(long ticks) {
        return "%02d.%02d".formatted(ticks / 20, (ticks % 20) * 5);
    }
}
