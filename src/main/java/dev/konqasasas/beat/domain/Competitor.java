package dev.konqasasas.beat.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Competitor {
    private final UUID uuid;
    private final String tournamentName;
    private final Set<CompetitionKind> participations = EnumSet.noneOf(CompetitionKind.class);
    private boolean overallExcluded;
    private boolean disqualified;

    public Competitor(UUID uuid, String tournamentName) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.tournamentName = requireName(tournamentName);
    }

    public UUID uuid() {
        return uuid;
    }

    public String tournamentName() {
        return tournamentName;
    }

    public void markParticipated(CompetitionKind competition) {
        participations.add(Objects.requireNonNull(competition, "competition"));
    }

    public boolean participatedIn(CompetitionKind competition) {
        return participations.contains(Objects.requireNonNull(competition, "competition"));
    }

    public boolean participatedInAllCompetitions() {
        return participations.size() == CompetitionKind.values().length;
    }

    public boolean overallExcluded() {
        return overallExcluded;
    }

    public void setOverallExcluded(boolean overallExcluded) {
        this.overallExcluded = overallExcluded;
    }

    public boolean disqualified() {
        return disqualified;
    }

    public void setDisqualified(boolean disqualified) {
        this.disqualified = disqualified;
    }

    public boolean eligibleForOverall() {
        return participatedInAllCompetitions() && !overallExcluded && !disqualified;
    }

    private static String requireName(String name) {
        Objects.requireNonNull(name, "tournamentName");
        if (name.isBlank()) {
            throw new IllegalArgumentException("tournamentName must not be blank");
        }
        return name;
    }
}
