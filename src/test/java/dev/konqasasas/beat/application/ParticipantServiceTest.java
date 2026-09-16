package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.JsonEventStateRepository;
import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import dev.konqasasas.beat.roster.GsonIdentityFileLoader;
import dev.konqasasas.beat.roster.RosterService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParticipantServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void firstParticipantLoginPersistsNameAndLaterNamesDoNotReplaceIt() throws Exception {
        UUID participantUuid = uuid("Participant");
        RosterService rosters = rosters(participantUuid, "OldRegisteredName", null);
        JsonEventStateRepository repository = new JsonEventStateRepository(
                temporaryDirectory.resolve("data/event-state.json"));
        EventStateSnapshot initial = initialState();
        repository.save(initial);
        EventStateService eventState = new EventStateService(repository, initial);
        ParticipantService participants = new ParticipantService(rosters, eventState);

        ParticipantLoginResult first = participants.handleLogin(participantUuid, "CurrentName");
        ParticipantLoginResult second = participants.handleLogin(participantUuid, "AnotherName");

        assertEquals(ParticipantLoginResult.Role.PARTICIPANT, first.role());
        assertTrue(first.registeredNameMismatch());
        assertEquals("CurrentName", first.tournamentName());
        assertEquals("CurrentName", second.tournamentName());
        assertEquals(
                "CurrentName",
                repository.load().orElseThrow().tournamentNames().get(participantUuid));
    }

    @Test
    void adminsAndUnknownPlayersAreNotSnapshottedAsParticipants() throws Exception {
        UUID adminUuid = uuid("Admin");
        RosterService rosters = rosters(null, null, adminUuid);
        JsonEventStateRepository repository = new JsonEventStateRepository(
                temporaryDirectory.resolve("event-state.json"));
        EventStateSnapshot initial = initialState();
        EventStateService eventState = new EventStateService(repository, initial);
        ParticipantService participants = new ParticipantService(rosters, eventState);

        assertEquals(
                ParticipantLoginResult.Role.ADMIN,
                participants.handleLogin(adminUuid, "AdminName").role());
        assertEquals(
                ParticipantLoginResult.Role.UNREGISTERED,
                participants.handleLogin(uuid("Unknown"), "Unknown").role());
        assertFalse(eventState.current().tournamentNames().containsKey(adminUuid));
        assertTrue(eventState.current().tournamentNames().isEmpty());
    }

    private RosterService rosters(UUID participantUuid, String participantName, UUID adminUuid)
            throws Exception {
        String participantJson = participantUuid == null
                ? "[]"
                : "[{\"uuid\":\"" + participantUuid + "\",\"mcid\":\""
                        + participantName + "\"}]";
        String adminJson = adminUuid == null
                ? "[]"
                : "[{\"uuid\":\"" + adminUuid + "\",\"mcid\":\"AdminName\"}]";
        Path participants = temporaryDirectory.resolve("participants.json");
        Path admins = temporaryDirectory.resolve("admins.json");
        Files.writeString(participants, participantJson, StandardCharsets.UTF_8);
        Files.writeString(admins, adminJson, StandardCharsets.UTF_8);
        return new RosterService(new GsonIdentityFileLoader(), participants, admins);
    }

    private static EventStateSnapshot initialState() {
        return new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION,
                TournamentState.WAITING,
                WhitelistMode.ADMIN_ONLY,
                Map.of());
    }

    private static UUID uuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
