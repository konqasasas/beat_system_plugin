package dev.konqasasas.beat.domain.ta;

import java.util.OptionalLong;

public final class TimeAttackRecord {
    private Long personalBestTicks;
    private Long personalBestRecordedTick;
    private Long personalBestRecordSequence;
    private Long personalBestSplit1Ticks;
    private Long personalBestSplit2Ticks;
    private boolean frozen;

    public PersonalBestUpdate recordGoal(
            long elapsedTicks,
            long competitionTick,
            long recordSequence,
            Long split1Ticks,
            Long split2Ticks) {
        validateRecord(elapsedTicks, competitionTick, recordSequence, split1Ticks, split2Ticks);
        Long previousBest = personalBestTicks;
        if (frozen || (previousBest != null && elapsedTicks >= previousBest)) {
            return new PersonalBestUpdate(previousBest, previousBest, false);
        }

        personalBestTicks = elapsedTicks;
        personalBestRecordedTick = competitionTick;
        personalBestRecordSequence = recordSequence;
        personalBestSplit1Ticks = split1Ticks;
        personalBestSplit2Ticks = split2Ticks;
        return new PersonalBestUpdate(previousBest, personalBestTicks, true);
    }

    public boolean hasPersonalBest() {
        return personalBestTicks != null;
    }

    public OptionalLong personalBestTicks() {
        return optional(personalBestTicks);
    }

    public OptionalLong personalBestRecordedTick() {
        return optional(personalBestRecordedTick);
    }

    public OptionalLong personalBestRecordSequence() {
        return optional(personalBestRecordSequence);
    }

    public OptionalLong personalBestSplit1Ticks() {
        return optional(personalBestSplit1Ticks);
    }

    public OptionalLong personalBestSplit2Ticks() {
        return optional(personalBestSplit2Ticks);
    }

    public void freeze() {
        frozen = true;
    }

    public boolean frozen() {
        return frozen;
    }

    private static OptionalLong optional(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    private static void validateRecord(
            long elapsedTicks,
            long competitionTick,
            long recordSequence,
            Long split1Ticks,
            Long split2Ticks) {
        if (elapsedTicks < 0 || competitionTick < 0) {
            throw new IllegalArgumentException("Tick values must not be negative");
        }
        if (recordSequence <= 0) {
            throw new IllegalArgumentException("recordSequence must be positive");
        }
        if ((split1Ticks != null && split1Ticks < 0)
                || (split2Ticks != null && split2Ticks < 0)) {
            throw new IllegalArgumentException("Split ticks must not be negative");
        }
    }

    public record PersonalBestUpdate(Long previousBestTicks, Long currentBestTicks, boolean updated) {
    }
}
