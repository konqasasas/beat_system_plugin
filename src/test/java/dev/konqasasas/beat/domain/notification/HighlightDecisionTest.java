package dev.konqasasas.beat.domain.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class HighlightDecisionTest {
    @Test void selectsOnlyTheMostImportantTagForCombinedEvent() {
        assertEquals(HighlightType.LEADER_CHANGE, HighlightDecision.primary(List.of(
                HighlightType.PERSONAL_BEST, HighlightType.BORDER,
                HighlightType.RANK_CHANGE, HighlightType.LEADER_CHANGE)).orElseThrow());
        assertEquals(HighlightType.ALL_CLEAR, HighlightDecision.primary(List.of(
                HighlightType.ALL_CLEAR, HighlightType.LEADER_UPDATE,
                HighlightType.COURSE_CLEAR)).orElseThrow());
    }
}
