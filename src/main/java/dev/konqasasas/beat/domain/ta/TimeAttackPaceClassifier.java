package dev.konqasasas.beat.domain.ta;

public final class TimeAttackPaceClassifier {
    private TimeAttackPaceClassifier() { }

    public static TimeAttackPace classify(long elapsed, Long personalBestSplit,
            Long borderSplit, Long leaderSplit) {
        if (faster(elapsed, leaderSplit)) return TimeAttackPace.LEADER;
        if (faster(elapsed, borderSplit)) return TimeAttackPace.BORDER;
        if (faster(elapsed, personalBestSplit)) return TimeAttackPace.PERSONAL_BEST;
        return TimeAttackPace.NONE;
    }

    private static boolean faster(long elapsed, Long reference) {
        return reference != null && elapsed < reference;
    }
}
