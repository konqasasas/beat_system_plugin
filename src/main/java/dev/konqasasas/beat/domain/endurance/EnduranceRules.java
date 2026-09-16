package dev.konqasasas.beat.domain.endurance;

public record EnduranceRules(int zone2Progress, int zone3Progress, int goalProgress) {
    public EnduranceRules {
        if (zone2Progress < 1
                || zone2Progress >= zone3Progress
                || zone3Progress >= goalProgress) {
            throw new IllegalArgumentException(
                    "Progress numbers must satisfy 1 <= zone2 < zone3 < goal");
        }
    }
}
