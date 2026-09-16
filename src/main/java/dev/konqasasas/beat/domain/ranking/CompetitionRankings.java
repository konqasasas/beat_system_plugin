package dev.konqasasas.beat.domain.ranking;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.endurance.EnduranceRecord;
import dev.konqasasas.beat.domain.high.HighDifficultyRecord;
import dev.konqasasas.beat.domain.ta.TimeAttackRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class CompetitionRankings {
    private CompetitionRankings() {
    }

    public static List<RankedEntry<HighDifficultyRecord>> highDifficulty(
            List<RankingEntry<HighDifficultyRecord>> entries) {
        Comparator<HighDifficultyRecord> recordOrder = (left, right) -> {
            int pointsOrder = Integer.compare(right.points(), left.points());
            if (pointsOrder != 0 || left.points() == 0) {
                return pointsOrder;
            }
            return Long.compare(
                    left.finalPointTick().orElseThrow(),
                    right.finalPointTick().orElseThrow());
        };
        return rank(entries, RankingEntry::record, recordOrder);
    }

    public static List<RankedEntry<TimeAttackRecord>> timeAttack(
            List<RankingEntry<TimeAttackRecord>> entries) {
        Comparator<TimeAttackRecord> recordOrder = (left, right) -> {
            if (!left.hasPersonalBest() || !right.hasPersonalBest()) {
                if (left.hasPersonalBest() == right.hasPersonalBest()) {
                    return 0;
                }
                return left.hasPersonalBest() ? -1 : 1;
            }

            int timeOrder = Long.compare(
                    left.personalBestTicks().orElseThrow(),
                    right.personalBestTicks().orElseThrow());
            if (timeOrder != 0) {
                return timeOrder;
            }
            int tickOrder = Long.compare(
                    left.personalBestRecordedTick().orElseThrow(),
                    right.personalBestRecordedTick().orElseThrow());
            if (tickOrder != 0) {
                return tickOrder;
            }
            return Long.compare(
                    left.personalBestRecordSequence().orElseThrow(),
                    right.personalBestRecordSequence().orElseThrow());
        };
        return rank(entries, RankingEntry::record, recordOrder);
    }

    public static List<RankedEntry<EnduranceRecord>> endurance(
            List<RankingEntry<EnduranceRecord>> entries) {
        Comparator<EnduranceRecord> recordOrder = (left, right) -> {
            int progressOrder = Integer.compare(right.maxProgress(), left.maxProgress());
            if (progressOrder != 0 || left.maxProgress() == 0) {
                return progressOrder;
            }
            return Long.compare(
                    left.maxProgressReachedTick().orElseThrow(),
                    right.maxProgressReachedTick().orElseThrow());
        };
        return rank(entries, RankingEntry::record, recordOrder);
    }

    private static <R> List<RankedEntry<R>> rank(
            List<RankingEntry<R>> source,
            Function<RankingEntry<R>, R> recordExtractor,
            Comparator<R> recordOrder) {
        List<RankingEntry<R>> sorted = new ArrayList<>(source);
        Comparator<RankingEntry<R>> entryOrder = Comparator
                .comparing(recordExtractor, recordOrder)
                .thenComparing(entry -> entry.competitor().uuid());
        sorted.sort(entryOrder);

        List<RankedEntry<R>> ranked = new ArrayList<>(sorted.size());
        int currentRank = 0;
        RankingEntry<R> previous = null;
        for (int index = 0; index < sorted.size(); index++) {
            RankingEntry<R> current = sorted.get(index);
            if (previous == null
                    || recordOrder.compare(previous.record(), current.record()) != 0) {
                currentRank = index + 1;
            }
            ranked.add(new RankedEntry<>(current.competitor(), current.record(), currentRank));
            previous = current;
        }
        return List.copyOf(ranked);
    }
}
