package dev.konqasasas.beat.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageTemplatesTest {
    @Test
    void replacesKnownPlaceholdersAndKeepsUnknownOnesVisible() {
        assertEquals(
                "Alex の順位は 3 位です ({missing})",
                MessageTemplates.render(
                        "{player} の順位は {rank} 位です ({missing})",
                        Map.of("player", "Alex", "rank", 3)));
    }
}
