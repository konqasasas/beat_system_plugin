package dev.konqasasas.beat.setup;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import dev.konqasasas.beat.spigot.EnduranceMarkerService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class RegionVisualizer {
    private final BeatPlugin plugin;
    private final ConfigurationFiles configuration;
    private final EnduranceMarkerService markers;

    public RegionVisualizer(BeatPlugin plugin, ConfigurationFiles configuration, EnduranceMarkerService markers) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.markers = markers;
    }

    public int show(Player player, List<RegionVisualization> regions) {
        int displayTicks = configuration.styleInt("setup-visualization.display-ticks", 100, 5, 1200);
        int intervalTicks = configuration.styleInt("setup-visualization.interval-ticks", 5, 1, 100);
        int perTick = configuration.styleInt("setup-visualization.max-particles-per-tick", 200, 1, 2000);
        double spacing = configuration.styleDouble("setup-visualization.spacing", 1.0D, 0.25D);

        List<ParticlePoint> points = new ArrayList<>();
        for (RegionVisualization visual : regions) {
            if (visual.region().world().equals(player.getWorld().getName())) {
                addPerimeter(points, visual, spacing);
            }
        }
        if (points.isEmpty()) return 0;
        List<ParticlePoint> immutable = List.copyOf(points);
        new BukkitRunnable() {
            private int elapsed;
            private Iterator<ParticlePoint> iterator = immutable.iterator();

            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= displayTicks) {
                    cancel();
                    return;
                }
                if (!iterator.hasNext()) iterator = immutable.iterator();
                int emitted = 0;
                while (iterator.hasNext() && emitted++ < perTick) {
                    ParticlePoint point = iterator.next();
                    player.spawnParticle(point.particle(), point.x(), point.y(), point.z(), 1, 0, 0, 0, 0);
                }
                elapsed += intervalTicks;
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);
        return regions.stream().map(RegionVisualization::region)
                .filter(region -> region.world().equals(player.getWorld().getName())).toList().size();
    }

    public int showEndurancePoints(Player player,
            Supplier<Map<Integer, List<EnduranceProgressPoint>>> points,
            boolean persistent) {
        int displayTicks = configuration.styleInt("setup-visualization.display-ticks", 100, 5, 1200);
        return markers.showSetup(player, points, persistent, displayTicks);
    }

    public boolean hideEndurancePoints(Player player) {
        return markers.hideSetup(player);
    }

    public Particle particle(String configuredName, Particle fallback) {
        try {
            return Particle.valueOf(configuredName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public Particle configuredParticle(String kind, Particle fallback) {
        return configuration.particle("setup-visualization.particles." + kind, fallback);
    }

    private static void addPerimeter(List<ParticlePoint> points, RegionVisualization visual, double spacing) {
        var r = visual.region();
        double y = r.y() + 1.02D;
        for (double x = r.minX(); x <= r.maxX() + 1.0D; x += spacing) {
            points.add(new ParticlePoint(x, y, r.minZ(), visual.particle()));
            points.add(new ParticlePoint(x, y, r.maxZ() + 1.0D, visual.particle()));
        }
        for (double z = r.minZ() + spacing; z < r.maxZ() + 1.0D; z += spacing) {
            points.add(new ParticlePoint(r.minX(), y, z, visual.particle()));
            points.add(new ParticlePoint(r.maxX() + 1.0D, y, z, visual.particle()));
        }
    }

    private record ParticlePoint(double x, double y, double z, Particle particle) {}
}
