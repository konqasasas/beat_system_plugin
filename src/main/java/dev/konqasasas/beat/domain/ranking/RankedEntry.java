package dev.konqasasas.beat.domain.ranking;

import dev.konqasasas.beat.domain.Competitor;

public record RankedEntry<R>(Competitor competitor, R record, int rank) {
    public boolean disqualified() {
        return competitor.disqualified();
    }
}
