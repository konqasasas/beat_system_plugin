package dev.konqasasas.beat.domain.notification;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public final class HighlightDecision {
    private HighlightDecision() { }
    public static Optional<HighlightType> primary(Collection<HighlightType> types) {
        return types.stream().max(Comparator.comparingInt(HighlightType::priority));
    }
}
