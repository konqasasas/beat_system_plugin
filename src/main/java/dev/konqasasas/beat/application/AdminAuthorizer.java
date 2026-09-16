package dev.konqasasas.beat.application;

import dev.konqasasas.beat.roster.RosterService;
import java.util.Objects;
import java.util.UUID;

public final class AdminAuthorizer {
    private final RosterService rosters;

    public AdminAuthorizer(RosterService rosters) {
        this.rosters = Objects.requireNonNull(rosters, "rosters");
    }

    public boolean isAdmin(UUID uuid) {
        return rosters.current().admin(uuid).isPresent();
    }
}
