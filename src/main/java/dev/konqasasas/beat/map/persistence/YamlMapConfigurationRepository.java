package dev.konqasasas.beat.map.persistence;

import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.HighDifficultyMapConfig;
import dev.konqasasas.beat.map.TimeAttackMapConfig;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.nio.file.Path;
import java.util.Optional;

public final class YamlMapConfigurationRepository implements MapConfigurationRepository {
    private final AtomicYamlFileStore<HighDifficultyMapConfig> high;
    private final AtomicYamlFileStore<TimeAttackMapConfig> timeAttack;
    private final AtomicYamlFileStore<EnduranceMapConfig> endurance;

    public YamlMapConfigurationRepository(Path mapsDirectory) {
        high = new AtomicYamlFileStore<>(
                mapsDirectory.resolve("high-difficulty.yml"), HighDifficultyMapConfig.class);
        timeAttack = new AtomicYamlFileStore<>(
                mapsDirectory.resolve("time-attack.yml"), TimeAttackMapConfig.class);
        endurance = new AtomicYamlFileStore<>(
                mapsDirectory.resolve("endurance.yml"), EnduranceMapConfig.class);
    }

    @Override
    public Optional<HighDifficultyMapConfig> loadHigh() throws PersistenceException {
        return high.load();
    }

    @Override
    public Optional<TimeAttackMapConfig> loadTimeAttack() throws PersistenceException {
        return timeAttack.load();
    }

    @Override
    public Optional<EnduranceMapConfig> loadEndurance() throws PersistenceException {
        return endurance.load();
    }

    @Override
    public void saveHigh(HighDifficultyMapConfig config) throws PersistenceException {
        high.save(config);
    }

    @Override
    public void saveTimeAttack(TimeAttackMapConfig config) throws PersistenceException {
        timeAttack.save(config);
    }

    @Override
    public void saveEndurance(EnduranceMapConfig config) throws PersistenceException {
        endurance.save(config);
    }
}
