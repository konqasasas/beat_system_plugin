package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;

public interface LiveCompetitionClock {
    Optional<Snapshot> liveClock();
    void debugSetElapsedTick(long targetTick) throws PersistenceException;

    record Snapshot(String phase, long elapsedTick, long totalTicks, List<Long> eventTicks) {
        public Snapshot { eventTicks = List.copyOf(eventTicks); }

        public long nextTarget() {
            long next = eventTicks.stream().mapToLong(Long::longValue).filter(value -> value > elapsedTick)
                    .findFirst().orElse(totalTicks);
            return Math.max(elapsedTick, next - 200);
        }
    }
}
