package dev.konqasasas.beat.domain.ta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class TimeAttackPaceClassifierTest {
    @Test void appliesPriorityAndRequiresStrictlyFasterTime() {
        assertEquals(TimeAttackPace.LEADER, TimeAttackPaceClassifier.classify(90, 120L, 110L, 100L));
        assertEquals(TimeAttackPace.BORDER, TimeAttackPaceClassifier.classify(105, 120L, 110L, 100L));
        assertEquals(TimeAttackPace.PERSONAL_BEST, TimeAttackPaceClassifier.classify(115, 120L, 110L, null));
        assertEquals(TimeAttackPace.NONE, TimeAttackPaceClassifier.classify(120, 120L, null, null));
        assertEquals(TimeAttackPace.NONE, TimeAttackPaceClassifier.classify(50, null, null, null));
    }
}
