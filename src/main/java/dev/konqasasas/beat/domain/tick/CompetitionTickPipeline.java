package dev.konqasasas.beat.domain.tick;

import java.util.List;
import java.util.Objects;

public final class CompetitionTickPipeline<E> {
    private final TickPhaseHandler<E> handler;
    private long lastProcessedTick = -1;

    public CompetitionTickPipeline(TickPhaseHandler<E> handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    public void processTick(long tick, List<? extends E> events) {
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative");
        }
        if (tick <= lastProcessedTick) {
            throw new IllegalStateException(
                    "Ticks must be processed once in increasing order; last="
                            + lastProcessedTick + ", requested=" + tick);
        }

        List<E> immutableEvents = List.copyOf(events);
        handler.processEvents(tick, immutableEvents);
        handler.updateRankings(tick);
        handler.evaluateEliminations(tick);
        handler.evaluateFinish(tick);
        lastProcessedTick = tick;
    }

    public long lastProcessedTick() {
        return lastProcessedTick;
    }
}
