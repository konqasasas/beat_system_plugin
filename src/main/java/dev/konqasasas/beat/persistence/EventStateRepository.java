package dev.konqasasas.beat.persistence;

import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import java.util.Optional;

public interface EventStateRepository {
    Optional<EventStateSnapshot> load() throws PersistenceException;

    void save(EventStateSnapshot state) throws PersistenceException;
}
