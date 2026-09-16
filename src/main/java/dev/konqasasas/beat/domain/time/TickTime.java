package dev.konqasasas.beat.domain.time;

public final class TickTime {
    public static final int TICKS_PER_SECOND = 20;

    private TickTime() {
    }

    public static long ticks(int minutes, int seconds) {
        if (minutes < 0 || seconds < 0 || seconds >= 60) {
            throw new IllegalArgumentException("Time must use non-negative minutes and seconds 0-59");
        }
        return Math.addExact(
                Math.multiplyExact((long) minutes, 60L * TICKS_PER_SECOND),
                Math.multiplyExact((long) seconds, TICKS_PER_SECOND));
    }

    public static boolean isAcceptedAtInclusiveDeadline(long eventTick, long deadlineTick) {
        requireNonNegative(eventTick, "eventTick");
        requireNonNegative(deadlineTick, "deadlineTick");
        return eventTick <= deadlineTick;
    }

    public static String formatMinutesSeconds(long ticks) {
        requireNonNegative(ticks, "ticks");
        long totalSeconds = ticks / TICKS_PER_SECOND;
        return "%02d:%02d".formatted(totalSeconds / 60, totalSeconds % 60);
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
