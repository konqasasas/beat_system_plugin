package dev.konqasasas.beat.application;

import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RegisteredIdentity;
import dev.konqasasas.beat.roster.RosterService;
import java.util.Objects;
import java.util.UUID;

public final class ParticipantService {
    private final RosterService rosters;
    private final EventStateService eventState;

    public ParticipantService(RosterService rosters, EventStateService eventState) {
        this.rosters = Objects.requireNonNull(rosters, "rosters");
        this.eventState = Objects.requireNonNull(eventState, "eventState");
    }

    public ParticipantLoginResult handleLogin(UUID uuid, String currentName)
            throws PersistenceException {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(currentName, "currentName");
        var roster = rosters.current();
        if (roster.admin(uuid).isPresent()) {
            return new ParticipantLoginResult(
                    ParticipantLoginResult.Role.ADMIN, null, null, false);
        }

        RegisteredIdentity participant = roster.participant(uuid).orElse(null);
        if (participant == null) {
            return new ParticipantLoginResult(
                    ParticipantLoginResult.Role.UNREGISTERED, null, null, false);
        }

        String tournamentName = eventState.recordTournamentNameIfAbsent(uuid, currentName);
        return new ParticipantLoginResult(
                ParticipantLoginResult.Role.PARTICIPANT,
                tournamentName,
                participant.mcid(),
                !participant.mcid().equals(currentName));
    }
}
