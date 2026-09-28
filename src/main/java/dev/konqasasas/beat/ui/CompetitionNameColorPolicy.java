package dev.konqasasas.beat.ui;

public final class CompetitionNameColorPolicy {
    private CompetitionNameColorPolicy() { }

    public static CompetitionNameColor high(int course, boolean allClear, boolean eliminated) {
        if (eliminated) return CompetitionNameColor.ELIMINATED;
        if (allClear) return CompetitionNameColor.COMPLETE;
        return switch (Math.max(1, Math.min(5, course))) {
            case 1 -> CompetitionNameColor.NEUTRAL;
            case 2 -> CompetitionNameColor.CAUTION;
            case 3 -> CompetitionNameColor.ACTIVE;
            case 4 -> CompetitionNameColor.ADVANCED;
            default -> CompetitionNameColor.LEADER;
        };
    }

    public static CompetitionNameColor endurance(boolean zone2, boolean zone3, boolean goal,
            boolean eliminated) {
        if (eliminated) return CompetitionNameColor.ELIMINATED;
        if (goal) return CompetitionNameColor.COMPLETE;
        if (zone3) return CompetitionNameColor.ADVANCED;
        if (zone2) return CompetitionNameColor.ACTIVE;
        return CompetitionNameColor.NEUTRAL;
    }

    public static CompetitionNameColor timeAttack(int rank, boolean hasRecord, boolean eliminated,
            int completedCutoffs, int firstSurvivors, int finalSurvivors) {
        if (eliminated) return CompetitionNameColor.ELIMINATED;
        if (!hasRecord) return CompetitionNameColor.DANGER;
        if (completedCutoffs <= 0) {
            if (rank <= finalSurvivors) return CompetitionNameColor.COMPLETE;
            if (rank <= firstSurvivors) return CompetitionNameColor.CAUTION;
            return CompetitionNameColor.DANGER;
        }
        if (completedCutoffs == 1) {
            return rank <= finalSurvivors ? CompetitionNameColor.COMPLETE : CompetitionNameColor.DANGER;
        }
        return CompetitionNameColor.COMPLETE;
    }
}
