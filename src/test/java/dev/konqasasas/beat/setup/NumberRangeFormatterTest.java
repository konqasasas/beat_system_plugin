package dev.konqasasas.beat.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class NumberRangeFormatterTest {
    @Test
    void formatsCompactRanges() {
        assertEquals("001-003, 005, 008-009", NumberRangeFormatter.format(List.of(3, 1, 2, 5, 8, 9)));
    }

    @Test
    void reportsGapsUpToMaximum() {
        assertEquals("003, 005-006", NumberRangeFormatter.missing(List.of(1, 2, 4, 7)));
    }
}
