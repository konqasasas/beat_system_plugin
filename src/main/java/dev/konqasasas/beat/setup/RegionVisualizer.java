package dev.konqasasas.beat.setup;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import dev.konqasasas.beat.ui.DustSphereRenderer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class RegionVisualizer {
    private final BeatPlugin plugin;
    private final ConfigurationFiles configuration;
    private final Map<UUID, BukkitTask> enduranceDisplays = new ConcurrentHashMap<>();

    public RegionVisualizer(BeatPlugin plugin, ConfigurationFiles configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
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

    public int showEndurancePoints(Player player, Supplier<List<EnduranceProgressPoint>> source, boolean persistent) {
        int displayTicks = configuration.styleInt("setup-visualization.display-ticks", 100, 5, 1200);
        int intervalTicks = configuration.styleInt("setup-visualization.interval-ticks", 5, 1, 100);
        int particleLimit = configuration.styleInt(
                "setup-visualization.endurance-progress.max-particles-per-tick",
                2000,
                DustSphereRenderer.PARTICLES_PER_SPHERE,
                20000);
        int pointsPerTick = Math.max(1, particleLimit / DustSphereRenderer.PARTICLES_PER_SPHERE);
        Particle.DustOptions dust = configuration.dustOptions(
                "setup-visualization.endurance-progress", Color.fromRGB(255, 200, 40), 0.7F);
        hideEndurancePoints(player);
        List<EnduranceProgressPoint> initial = visibleEndurancePoints(player, source.get());
        if (initial.isEmpty()) return 0;

        BukkitTask task = new BukkitRunnable() {
            private int elapsed;
            private int offset;

            @Override
            public void run() {
                if (!player.isOnline() || !persistent && elapsed >= displayTicks) {
                    enduranceDisplays.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                List<EnduranceProgressPoint> points = visibleEndurancePoints(player, source.get());
                if (!points.isEmpty()) {
                    int count = Math.min(pointsPerTick, points.size());
                    for (int index = 0; index < count; index++) {
                        DustSphereRenderer.spawn(player, points.get((offset + index) % points.size()), dust);
                    }
                    offset = (offset + count) % points.size();
                }
                elapsed += intervalTicks;
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);
        enduranceDisplays.put(player.getUniqueId(), task);
        return initial.size();
    }

    public boolean hideEndurancePoints(Player player) {
        BukkitTask task = enduranceDisplays.remove(player.getUniqueId());
        if (task == null) return false;
        task.cancel();
        return true;
    }

    private static List<EnduranceProgressPoint> visibleEndurancePoints(
            Player player, List<EnduranceProgressPoint> points) {
        return points.stream()
                .filter(point -> point.world().equals(player.getWorld().getName()))
                .toList();
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
