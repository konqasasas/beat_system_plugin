package dev.konqasasas.beat.domain.tick;

import java.util.List;

public interface TickPhaseHandler<E> {
    void processEvents(long tick, List<E> events);

    void updateRankings(long tick);

    void evaluateEliminations(long tick);

    void evaluateFinish(long tick);
}
