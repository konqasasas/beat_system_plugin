package dev.konqasasas.beat.application;

import dev.konqasasas.beat.domain.WhitelistMode;

public record WhitelistStatus(
        WhitelistMode mode,
        int configuredPlayers,
        int whitelistedPlayers,
        boolean synchronizedExactly) {
}
