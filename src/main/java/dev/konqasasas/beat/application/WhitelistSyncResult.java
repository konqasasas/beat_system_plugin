package dev.konqasasas.beat.application;

import dev.konqasasas.beat.domain.WhitelistMode;

public record WhitelistSyncResult(
        WhitelistMode mode,
        int configuredPlayers,
        int added,
        int removed) {
}
