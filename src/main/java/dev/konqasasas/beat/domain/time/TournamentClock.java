package dev.konqasasas.beat.domain.time;

public final class TournamentClock {
    private long elapsedTicks;
    private boolean running;

    public void start() {
        if (running) {
            throw new IllegalStateException("Tournament clock is already running");
        }
        elapsedTicks = 0;
        running = true;
    }

    public long advanceOneTick() {
        requireRunning();
        elapsedTicks = Math.incrementExact(elapsedTicks);
        return elapsedTicks;
    }

    public void stop() {
        running = false;
    }

    public long elapsedTicks() {
        return elapsedTicks;
    }

    public boolean running() {
        return running;
    }

    public boolean isAt(long tick) {
        return elapsedTicks == tick;
    }

    public boolean acceptsEventThrough(long inclusiveDeadlineTick) {
        return TickTime.isAcceptedAtInclusiveDeadline(elapsedTicks, inclusiveDeadlineTick);
    }

    private void requireRunning() {
        if (!running) {
            throw new IllegalStateException("Tournament clock is not running");
        }
    }
}
