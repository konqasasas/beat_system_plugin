package dev.konqasasas.beat.roster;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RosterSnapshot {
    private final Map<UUID, RegisteredIdentity> participants;
    private final Map<UUID, RegisteredIdentity> admins;

    private RosterSnapshot(
            Map<UUID, RegisteredIdentity> participants,
            Map<UUID, RegisteredIdentity> admins) {
        this.participants = Map.copyOf(participants);
        this.admins = Map.copyOf(admins);
    }

    public static RosterSnapshot create(
            List<RegisteredIdentity> participants,
            List<RegisteredIdentity> admins) throws RosterException {
        Map<UUID, RegisteredIdentity> participantMap = uniqueByUuid(participants, "participants");
        Map<UUID, RegisteredIdentity> adminMap = uniqueByUuid(admins, "admins");
        return new RosterSnapshot(participantMap, adminMap);
    }

    public Map<UUID, RegisteredIdentity> participants() {
        return participants;
    }

    public Map<UUID, RegisteredIdentity> admins() {
        return admins;
    }

    public Set<UUID> participantUuids() {
        return participants.keySet();
    }

    public Set<UUID> adminUuids() {
        return admins.keySet();
    }

    public Optional<RegisteredIdentity> participant(UUID uuid) {
        return Optional.ofNullable(participants.get(uuid));
    }

    public Optional<RegisteredIdentity> admin(UUID uuid) {
        return Optional.ofNullable(admins.get(uuid));
    }

    public Optional<RegisteredIdentity> findParticipant(String name) {
        return participants.values().stream()
                .filter(identity -> identity.mcid().equalsIgnoreCase(name))
                .findFirst();
    }

    private static Map<UUID, RegisteredIdentity> uniqueByUuid(
            List<RegisteredIdentity> identities,
            String sourceName) throws RosterException {
        LinkedHashMap<UUID, RegisteredIdentity> result = new LinkedHashMap<>();
        for (RegisteredIdentity identity : identities) {
            RegisteredIdentity previous = result.putIfAbsent(identity.uuid(), identity);
            if (previous != null) {
                throw new RosterException("Duplicate UUID in " + sourceName + ": " + identity.uuid());
            }
        }
        return result;
    }
}
