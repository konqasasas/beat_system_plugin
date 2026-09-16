package dev.konqasasas.beat.application;

import java.util.Set;
import java.util.UUID;

public interface WhitelistGateway {
    Set<UUID> whitelistedUuids();

    void setWhitelisted(UUID uuid, boolean whitelisted);

    void enableWhitelist();
}
