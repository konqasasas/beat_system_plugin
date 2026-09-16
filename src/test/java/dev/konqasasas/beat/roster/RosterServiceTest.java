package dev.konqasasas.beat.roster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RosterServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void validJsonLoadsUuidBasedParticipantAndAdminRegistries() throws Exception {
        UUID participant = uuid("Participant");
        UUID admin = uuid("Admin");
        Path participants = write("participants.json", json(participant, "PlayerA"));
        Path admins = write("admins.json", json(admin, "AdminA"));

        RosterService service = new RosterService(
                new GsonIdentityFileLoader(), participants, admins);

        assertEquals("PlayerA", service.current().participant(participant).orElseThrow().mcid());
        assertEquals("AdminA", service.current().admin(admin).orElseThrow().mcid());
    }

    @Test
    void invalidReloadKeepsTheLastKnownGoodRoster() throws Exception {
        UUID participant = uuid("Participant");
        Path participants = write("participants.json", json(participant, "PlayerA"));
        Path admins = write("admins.json", "[]");
        RosterService service = new RosterService(
                new GsonIdentityFileLoader(), participants, admins);
        Files.writeString(participants, "{ invalid", StandardCharsets.UTF_8);

        assertThrows(RosterException.class, service::reload);

        assertTrue(service.current().participant(participant).isPresent());
        assertEquals(1, service.current().participants().size());
    }

    @Test
    void duplicateUuidsWithinOneRosterAreRejectedButAdminParticipantRoleMayOverlap() throws Exception {
        UUID uuid = uuid("SamePerson");
        Path participants = write("participants.json", "["
                + entry(uuid, "PlayerA") + "," + entry(uuid, "PlayerB") + "]");
        Path admins = write("admins.json", "[]");
        assertThrows(RosterException.class, () -> new RosterService(
                new GsonIdentityFileLoader(), participants, admins));

        Files.writeString(participants, json(uuid, "PlayerA"), StandardCharsets.UTF_8);
        Files.writeString(admins, json(uuid, "AdminA"), StandardCharsets.UTF_8);
        RosterService service = new RosterService(
                new GsonIdentityFileLoader(), participants, admins);
        assertTrue(service.current().participant(uuid).isPresent());
        assertTrue(service.current().admin(uuid).isPresent());
    }

    @Test
    void compactUuidWithoutHyphensIsAccepted() throws Exception {
        UUID uuid = uuid("CompactUuid");
        Path participants = write("participants.json", "[{\"uuid\":\""
                + uuid.toString().replace("-", "") + "\",\"mcid\":\"PlayerA\"}]");
        Path admins = write("admins.json", "[]");

        RosterService service = new RosterService(
                new GsonIdentityFileLoader(), participants, admins);

        assertEquals("PlayerA", service.current().participant(uuid).orElseThrow().mcid());
    }

    @Test
    void malformedUuidAndBlankNameAreRejected() throws Exception {
        Path participants = write(
                "participants.json", "[{\"uuid\":\"not-a-uuid\",\"mcid\":\"PlayerA\"}]");
        Path admins = write("admins.json", "[]");
        assertThrows(RosterException.class, () -> new RosterService(
                new GsonIdentityFileLoader(), participants, admins));

        Files.writeString(
                participants,
                "[{\"uuid\":\"" + UUID.randomUUID() + "\",\"mcid\":\" \"}]",
                StandardCharsets.UTF_8);
        assertThrows(RosterException.class, () -> new RosterService(
                new GsonIdentityFileLoader(), participants, admins));
    }

    private Path write(String name, String content) throws Exception {
        Path file = temporaryDirectory.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String json(UUID uuid, String mcid) {
        return "[" + entry(uuid, mcid) + "]";
    }

    private static String entry(UUID uuid, String mcid) {
        return "{\"uuid\":\"" + uuid + "\",\"mcid\":\"" + mcid + "\"}";
    }

    private static UUID uuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
