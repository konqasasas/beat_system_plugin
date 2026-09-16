package dev.konqasasas.beat.persistence;

import dev.konqasasas.beat.persistence.snapshot.EventStateSnapshot;
import java.nio.file.Path;
import java.util.Optional;

public final class JsonEventStateRepository implements EventStateRepository {
    private final AtomicJsonFileStore<EventStateSnapshot> store;

    public JsonEventStateRepository(Path file) {
        store = new AtomicJsonFileStore<>(file, EventStateSnapshot.class, JsonSupport.createGson());
    }

    @Override
    public Optional<EventStateSnapshot> load() throws PersistenceException {
        return store.load();
    }

    @Override
    public void save(EventStateSnapshot state) throws PersistenceException {
        store.save(state);
    }
}
