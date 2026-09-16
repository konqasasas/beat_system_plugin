package dev.konqasasas.beat.domain.ranking;

import dev.konqasasas.beat.domain.Competitor;
import java.util.Objects;

public record RankingEntry<R>(Competitor competitor, R record) {
    public RankingEntry {
        Objects.requireNonNull(competitor, "competitor");
        Objects.requireNonNull(record, "record");
    }
}
