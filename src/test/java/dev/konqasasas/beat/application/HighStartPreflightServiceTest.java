package dev.konqasasas.beat.application;

import static org.junit.jupiter.api.Assertions.*;
import dev.konqasasas.beat.configuration.CompetitionSettings;
import dev.konqasasas.beat.roster.RegisteredIdentity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HighStartPreflightServiceTest {
    @Test void invalidatesOnlyTheSnapshotThatChanged() {
        AtomicReference<CompetitionSettings> settings = new AtomicReference<>(CompetitionSettings.production());
        Map<UUID, RegisteredIdentity> roster = new HashMap<>();
        UUID admin = UUID.randomUUID(), player = UUID.randomUUID();
        roster.put(player, new RegisteredIdentity(player, "Player"));
        var service = new HighStartPreflightService(settings::get, () -> Map.copyOf(roster));
        service.confirmSettings(admin);service.confirmParticipants(admin);
        assertTrue(service.status(admin).ready());
        settings.set(CompetitionSettings.testPreset());
        assertFalse(service.status(admin).settingsConfirmed());
        assertTrue(service.status(admin).participantsConfirmed());
        service.confirmSettings(admin);
        UUID added=UUID.randomUUID();roster.put(added,new RegisteredIdentity(added,"Added"));
        assertTrue(service.status(admin).settingsConfirmed());
        assertFalse(service.status(admin).participantsConfirmed());
    }
}
