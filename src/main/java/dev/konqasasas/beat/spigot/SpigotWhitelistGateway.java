package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.application.WhitelistGateway;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

public final class SpigotWhitelistGateway implements WhitelistGateway {
    private final Server server;

    public SpigotWhitelistGateway(Server server) {
        this.server = server;
    }

    @Override
    public Set<UUID> whitelistedUuids() {
        return server.getWhitelistedPlayers().stream()
                .map(OfflinePlayer::getUniqueId)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void setWhitelisted(UUID uuid, boolean whitelisted) {
        server.getOfflinePlayer(uuid).setWhitelisted(whitelisted);
    }

    @Override
    public void enableWhitelist() {
        server.setWhitelist(true);
    }
}
