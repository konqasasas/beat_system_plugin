package dev.konqasasas.beat.roster;

import java.util.Objects;
import java.util.UUID;

public record RegisteredIdentity(UUID uuid, String mcid) {
    public RegisteredIdentity {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(mcid, "mcid");
        if (mcid.isBlank()) {
            throw new IllegalArgumentException("mcid must not be blank");
        }
    }
}
