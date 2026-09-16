package dev.konqasasas.beat.map.persistence;

import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.HighDifficultyMapConfig;
import dev.konqasasas.beat.map.TimeAttackMapConfig;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.Optional;

public interface MapConfigurationRepository {
    Optional<HighDifficultyMapConfig> loadHigh() throws PersistenceException;

    Optional<TimeAttackMapConfig> loadTimeAttack() throws PersistenceException;

    Optional<EnduranceMapConfig> loadEndurance() throws PersistenceException;

    void saveHigh(HighDifficultyMapConfig config) throws PersistenceException;

    void saveTimeAttack(TimeAttackMapConfig config) throws PersistenceException;

    void saveEndurance(EnduranceMapConfig config) throws PersistenceException;
}
