package dev.konqasasas.beat.domain.time;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public final class CompetitionSchedule {
    private final List<Long> eliminationTicks;
    private final long finishTick;

    public CompetitionSchedule(List<Long> eliminationTicks, long finishTick) {
        if (finishTick < 0) {
            throw new IllegalArgumentException("finishTick must not be negative");
        }

        List<Long> copy = new ArrayList<>(eliminationTicks);
        long previous = -1;
        for (long tick : copy) {
            if (tick < 0 || tick <= previous || tick >= finishTick) {
                throw new IllegalArgumentException(
                        "Elimination ticks must be unique, increasing, and before finishTick");
            }
            previous = tick;
        }
        this.eliminationTicks = List.copyOf(copy);
        this.finishTick = finishTick;
    }

    public List<Long> eliminationTicks() {
        return eliminationTicks;
    }

    public long finishTick() {
        return finishTick;
    }

    public boolean isEliminationTick(long tick) {
        return eliminationTicks.contains(tick);
    }

    public boolean isFinishTick(long tick) {
        return tick == finishTick;
    }

    public boolean acceptsEvents(long tick) {
        return TickTime.isAcceptedAtInclusiveDeadline(tick, finishTick);
    }

    public OptionalLong nextBoundaryAfter(long tick) {
        for (long eliminationTick : eliminationTicks) {
            if (eliminationTick > tick) {
                return OptionalLong.of(eliminationTick);
            }
        }
        return finishTick > tick ? OptionalLong.of(finishTick) : OptionalLong.empty();
    }
}
