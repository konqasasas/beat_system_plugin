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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WhitelistServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void adminModeExactlySynchronizesAndPersistsTheMode() throws Exception {
        UUID participant = uuid("Participant");
        UUID admin = uuid("Admin");
        UUID unrelated = uuid("Unrelated");
        RosterService rosters = rosters(participant, admin);
        EventStateService eventState = eventState();
        FakeWhitelistGateway gateway = new FakeWhitelistGateway(Set.of(participant, unrelated));
        WhitelistService service = new WhitelistService(rosters, eventState, gateway);

        WhitelistSyncResult result = service.synchronize(WhitelistMode.ADMIN_ONLY);

        assertEquals(Set.of(admin), gateway.whitelistedUuids());
        assertEquals(1, result.added());
        assertEquals(2, result.removed());
        assertTrue(gateway.enabled);
        assertEquals(WhitelistMode.ADMIN_ONLY, eventState.current().whitelistMode());
        assertTrue(service.status().synchronizedExactly());
    }

    @Test
    void allModeContainsAdminsAndParticipantsAndSurvivesReload() throws Exception {
        UUID participant = uuid("Participant");
        UUID admin = uuid("Admin");
        RosterService rosters = rosters(participant, admin);
        EventStateService eventState = eventState();
        FakeWhitelistGateway gateway = new FakeWhitelistGateway(Set.of());
        WhitelistService service = new WhitelistService(rosters, eventState, gateway);

        service.synchronize(WhitelistMode.ALL);

        assertEquals(Set.of(participant, admin), gateway.whitelistedUuids());
        assertEquals(WhitelistMode.ALL, eventState.current().whitelistMode());
        EventStateSnapshot saved = new JsonEventStateRepository(
                temporaryDirectory.resolve("event-state.json")).load().orElseThrow();
        assertEquals(WhitelistMode.ALL, saved.whitelistMode());
        assertFalse(service.status().mode() == WhitelistMode.ADMIN_ONLY);
    }

    private RosterService rosters(UUID participant, UUID admin) throws Exception {
        Path participants = temporaryDirectory.resolve("participants.json");
        Path admins = temporaryDirectory.resolve("admins.json");
        Files.writeString(participants, json(participant, "PlayerA"), StandardCharsets.UTF_8);
        Files.writeString(admins, json(admin, "AdminA"), StandardCharsets.UTF_8);
        return new RosterService(new GsonIdentityFileLoader(), participants, admins);
    }

    private EventStateService eventState() throws Exception {
        JsonEventStateRepository repository = new JsonEventStateRepository(
                temporaryDirectory.resolve("event-state.json"));
        EventStateSnapshot initial = new EventStateSnapshot(
                EventStateSnapshot.CURRENT_SCHEMA_VERSION,
                TournamentState.WAITING,
                WhitelistMode.ADMIN_ONLY,
                Map.of());
        repository.save(initial);
        return new EventStateService(repository, initial);
    }

    private static String json(UUID uuid, String name) {
        return "[{\"uuid\":\"" + uuid + "\",\"mcid\":\"" + name + "\"}]";
    }

    private static UUID uuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static final class FakeWhitelistGateway implements WhitelistGateway {
        private final Set<UUID> entries;
        private boolean enabled;

        private FakeWhitelistGateway(Set<UUID> entries) {
            this.entries = new HashSet<>(entries);
        }

        @Override
        public Set<UUID> whitelistedUuids() {
            return Set.copyOf(entries);
        }

        @Override
        public void setWhitelisted(UUID uuid, boolean whitelisted) {
            if (whitelisted) {
                entries.add(uuid);
            } else {
                entries.remove(uuid);
            }
        }

        @Override
        public void enableWhitelist() {
            enabled = true;
        }
    }
}
