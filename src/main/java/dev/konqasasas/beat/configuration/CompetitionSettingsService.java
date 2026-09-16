package dev.konqasasas.beat.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class CompetitionSettingsService {
    private final JavaPlugin plugin;
    private final File file;
    private CompetitionSettings current;

    private CompetitionSettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "competition-settings.yml");
    }

    public static CompetitionSettingsService load(JavaPlugin plugin) throws ConfigurationLoadException {
        CompetitionSettingsService service = new CompetitionSettingsService(plugin);
        if (!service.file.isFile()) {
            service.current = service.importLegacy();
            service.save(service.current);
        } else {
            service.reload();
        }
        return service;
    }

    public synchronized CompetitionSettings current() { return current; }

    public synchronized void reload() throws ConfigurationLoadException { current = read(); }

    public synchronized void save(CompetitionSettings settings) throws ConfigurationLoadException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", 1);
        yaml.set("preset-note", "Values can be edited in-game with /beat settings or the admin menu.");
        yaml.set("countdowns.competition-start-ticks", settings.startCountdownTicks());
        yaml.set("timings.high-practice-ticks", settings.highPracticeTicks());
        yaml.set("timings.high-prepare-ticks", settings.highPrepareTicks());
        yaml.set("timings.high-running-ticks", settings.highRunningTicks());
        yaml.set("timings.ta-running-ticks", settings.timeAttackRunningTicks());
        yaml.set("timings.endurance-running-ticks", settings.enduranceRunningTicks());
        yaml.set("eliminations.high-ticks", settings.highEliminationTicks());
        yaml.set("eliminations.ta-ticks", settings.timeAttackEliminationTicks());
        yaml.set("eliminations.endurance-ticks", settings.enduranceEliminationTicks());
        yaml.set("ta.survivor-counts", settings.timeAttackSurvivorCounts());
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            yaml.save(temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            current = settings;
        } catch (IOException exception) {
            throw new ConfigurationLoadException("競技設定を保存できません: " + file.getName(), exception);
        } finally {
            try { Files.deleteIfExists(temporary.toPath()); } catch (IOException ignored) { }
        }
    }

    private CompetitionSettings read() throws ConfigurationLoadException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
            return from(yaml);
        } catch (IOException | InvalidConfigurationException | IllegalArgumentException exception) {
            throw new ConfigurationLoadException("競技設定を読み込めません: " + file.getName(), exception);
        }
    }

    private CompetitionSettings importLegacy() {
        var config = plugin.getConfig();
        return new CompetitionSettings(
                config.getLong("countdowns.competition-start-ticks", 200),
                config.getLong("timings.high-practice-ticks", 12_000),
                config.getLong("timings.high-prepare-ticks", 1_200),
                config.getLong("timings.high-running-ticks", 36_000),
                integers(config.getIntegerList("eliminations.high-ticks"), List.of(12_000, 18_000, 24_000, 30_000)),
                config.getLong("timings.ta-running-ticks", 36_000),
                integers(config.getIntegerList("eliminations.ta-ticks"), List.of(24_000, 30_000)),
                integers(config.getIntegerList("ta.survivor-counts"), List.of(20, 10)),
                config.getLong("timings.endurance-running-ticks", 36_000),
                integers(config.getIntegerList("eliminations.endurance-ticks"), List.of(24_000, 30_000)));
    }

    private static CompetitionSettings from(YamlConfiguration yaml) {
        return new CompetitionSettings(
                yaml.getLong("countdowns.competition-start-ticks"),
                yaml.getLong("timings.high-practice-ticks"),
                yaml.getLong("timings.high-prepare-ticks"),
                yaml.getLong("timings.high-running-ticks"), yaml.getIntegerList("eliminations.high-ticks"),
                yaml.getLong("timings.ta-running-ticks"), yaml.getIntegerList("eliminations.ta-ticks"),
                yaml.getIntegerList("ta.survivor-counts"),
                yaml.getLong("timings.endurance-running-ticks"), yaml.getIntegerList("eliminations.endurance-ticks"));
    }

    private static List<Integer> integers(List<Integer> value, List<Integer> fallback) {
        return value.isEmpty() ? fallback : value;
    }
}
