package dev.konqasasas.beat.map.persistence;

import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.HighDifficultyMapConfig;
import dev.konqasasas.beat.map.TimeAttackMapConfig;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.nio.file.Path;
import java.util.Objects;

public final class MapConfigurationService {
    private final MapConfigurationRepository repository;
    private HighDifficultyMapConfig high;
    private TimeAttackMapConfig timeAttack;
    private EnduranceMapConfig endurance;

    private MapConfigurationService(
            MapConfigurationRepository repository,
            HighDifficultyMapConfig high,
            TimeAttackMapConfig timeAttack,
            EnduranceMapConfig endurance) {
        this.repository = repository;
        this.high = high;
        this.timeAttack = timeAttack;
        this.endurance = endurance;
    }

    public static MapConfigurationService open(Path mapsDirectory) throws PersistenceException {
        return open(new YamlMapConfigurationRepository(mapsDirectory));
    }

    public static MapConfigurationService open(MapConfigurationRepository repository)
            throws PersistenceException {
        Objects.requireNonNull(repository, "repository");
        var loadedHigh = repository.loadHigh();
        var loadedTimeAttack = repository.loadTimeAttack();
        var loadedEndurance = repository.loadEndurance();
        HighDifficultyMapConfig high = loadedHigh.orElseGet(HighDifficultyMapConfig::empty);
        TimeAttackMapConfig timeAttack = loadedTimeAttack.orElseGet(TimeAttackMapConfig::empty);
        EnduranceMapConfig endurance = loadedEndurance.orElseGet(EnduranceMapConfig::empty);
        if (loadedHigh.isEmpty()) {
            repository.saveHigh(high);
        }
        if (loadedTimeAttack.isEmpty()) {
            repository.saveTimeAttack(timeAttack);
        }
        if (loadedEndurance.isEmpty()) {
            repository.saveEndurance(endurance);
        }
        return new MapConfigurationService(repository, high, timeAttack, endurance);
    }

    public synchronized HighDifficultyMapConfig high() {
        return high;
    }

    public synchronized TimeAttackMapConfig timeAttack() {
        return timeAttack;
    }

    public synchronized EnduranceMapConfig endurance() {
        return endurance;
    }

    public synchronized void reload() throws PersistenceException {
        HighDifficultyMapConfig candidateHigh = repository.loadHigh()
                .orElseThrow(() -> new PersistenceException("high-difficulty.yml is missing"));
        TimeAttackMapConfig candidateTimeAttack = repository.loadTimeAttack()
                .orElseThrow(() -> new PersistenceException("time-attack.yml is missing"));
        EnduranceMapConfig candidateEndurance = repository.loadEndurance()
                .orElseThrow(() -> new PersistenceException("endurance.yml is missing"));
        high = candidateHigh;
        timeAttack = candidateTimeAttack;
        endurance = candidateEndurance;
    }

    public synchronized void saveHigh(HighDifficultyMapConfig candidate)
            throws PersistenceException {
        repository.saveHigh(candidate);
        high = candidate;
    }

    public synchronized void saveTimeAttack(TimeAttackMapConfig candidate)
            throws PersistenceException {
        repository.saveTimeAttack(candidate);
        timeAttack = candidate;
    }

    public synchronized void saveEndurance(EnduranceMapConfig candidate)
            throws PersistenceException {
        repository.saveEndurance(candidate);
        endurance = candidate;
    }
}
