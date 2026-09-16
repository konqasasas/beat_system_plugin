package dev.konqasasas.beat.setup;

import dev.konqasasas.beat.map.BlockRegion;
import java.util.Objects;
import org.bukkit.Particle;

public record RegionVisualization(BlockRegion region, Particle particle) {
    public RegionVisualization {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(particle, "particle");
    }
}
