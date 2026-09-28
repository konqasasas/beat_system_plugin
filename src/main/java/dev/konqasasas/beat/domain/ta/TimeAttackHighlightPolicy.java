package dev.konqasasas.beat.domain.ta;

import dev.konqasasas.beat.domain.notification.HighlightType;
import java.util.ArrayList;
import java.util.List;

public final class TimeAttackHighlightPolicy {
    private TimeAttackHighlightPolicy() { }

    public static List<HighlightType> personalBest(Long previousBest, int previousRank,
            int currentRank, Integer nextBorderRank) {
        List<HighlightType> conditions = new ArrayList<>();
        conditions.add(HighlightType.PERSONAL_BEST);
        if (currentRank == 1) {
            conditions.add(previousBest == null || previousRank != 1
                    ? HighlightType.LEADER_CHANGE : HighlightType.LEADER_UPDATE);
        } else if (previousRank != currentRank) {
            conditions.add(HighlightType.RANK_CHANGE);
        }
        if (nextBorderRank != null && currentRank <= nextBorderRank
                && (previousBest == null || previousRank > nextBorderRank)) {
            conditions.add(HighlightType.BORDER);
        }
        return List.copyOf(conditions);
    }
}
