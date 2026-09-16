package dev.konqasasas.beat.application;

import dev.konqasasas.beat.domain.WhitelistMode;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RosterService;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WhitelistService {
    private final RosterService rosters;
    private final EventStateService eventState;
    private final WhitelistGateway gateway;

    public WhitelistService(
            RosterService rosters,
            EventStateService eventState,
            WhitelistGateway gateway) {
        this.rosters = Objects.requireNonNull(rosters, "rosters");
        this.eventState = Objects.requireNonNull(eventState, "eventState");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public synchronized WhitelistSyncResult synchronize(WhitelistMode mode)
            throws PersistenceException {
        Set<UUID> target = targetFor(mode);
        Set<UUID> current = new HashSet<>(gateway.whitelistedUuids());
        Set<UUID> additions = new HashSet<>(target);
        additions.removeAll(current);
        Set<UUID> removals = new HashSet<>(current);
        removals.removeAll(target);

        additions.forEach(uuid -> gateway.setWhitelisted(uuid, true));
        removals.forEach(uuid -> gateway.setWhitelisted(uuid, false));
        gateway.enableWhitelist();
        eventState.setWhitelistMode(mode);
        return new WhitelistSyncResult(mode, target.size(), additions.size(), removals.size());
    }

    public WhitelistStatus status() {
        WhitelistMode mode = eventState.current().whitelistMode();
        Set<UUID> target = targetFor(mode);
        Set<UUID> current = gateway.whitelistedUuids();
        return new WhitelistStatus(mode, target.size(), current.size(), target.equals(current));
    }

    private Set<UUID> targetFor(WhitelistMode mode) {
        Objects.requireNonNull(mode, "mode");
        Set<UUID> target = new HashSet<>(rosters.current().adminUuids());
        if (mode == WhitelistMode.ALL) {
            target.addAll(rosters.current().participantUuids());
        }
        return Set.copyOf(target);
    }
}
