package dev.konqasasas.beat.domain.ranking;

import dev.konqasasas.beat.domain.Competitor;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class SurvivorSelector {
    private SurvivorSelector() {
    }

    public static <R> Set<UUID> topEligible(
            List<RankedEntry<R>> ranking,
            int survivorCount,
            Predicate<R> hasQualifyingRecord) {
        if (survivorCount < 0) {
            throw new IllegalArgumentException("survivorCount must not be negative");
        }

        LinkedHashSet<UUID> survivors = new LinkedHashSet<>();
        for (RankedEntry<R> entry : ranking) {
            Competitor competitor = entry.competitor();
            if (competitor.disqualified() || !hasQualifyingRecord.test(entry.record())) {
                continue;
            }
            if (survivors.size() == survivorCount) {
                break;
            }
            survivors.add(competitor.uuid());
        }
        return Set.copyOf(survivors);
    }
}
