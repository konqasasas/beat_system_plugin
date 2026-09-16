package dev.konqasasas.beat.persistence;

import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.util.Optional;

public interface ResultsRepository {
    Optional<ResultsSnapshot> load() throws PersistenceException;

    void save(ResultsSnapshot results) throws PersistenceException;
}
