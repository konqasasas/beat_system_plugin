package dev.konqasasas.beat.spigot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FirstJoinTrackerTest {
    @Test
    void onlyAFirstSpawnTriggersOneJoin() {
        var tracker = new FirstJoinTracker();
        UUID firstTimePlayer = UUID.randomUUID();
        UUID returningPlayer = UUID.randomUUID();

        tracker.recordSpawn(firstTimePlayer, false);
        tracker.recordSpawn(returningPlayer, true);

        assertTrue(tracker.consumeJoin(firstTimePlayer));
        assertFalse(tracker.consumeJoin(firstTimePlayer));
        assertFalse(tracker.consumeJoin(returningPlayer));
    }
}
