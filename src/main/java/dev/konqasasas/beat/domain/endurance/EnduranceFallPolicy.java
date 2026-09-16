package dev.konqasasas.beat.domain.endurance;

public final class EnduranceFallPolicy {
    private EnduranceFallPolicy() {}

    public static boolean shouldRestart(EnduranceRecord record, double playerY, double fallY) {
        return !record.goalReached() && playerY <= fallY;
    }
}
