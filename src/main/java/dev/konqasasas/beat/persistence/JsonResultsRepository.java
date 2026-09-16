package dev.konqasasas.beat.persistence;

import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.nio.file.Path;
import java.util.Optional;

public final class JsonResultsRepository implements ResultsRepository {
    private final AtomicJsonFileStore<ResultsSnapshot> store;

    public JsonResultsRepository(Path file) {
        store = new AtomicJsonFileStore<>(file, ResultsSnapshot.class, JsonSupport.createGson());
    }

    @Override
    public Optional<ResultsSnapshot> load() throws PersistenceException {
        return store.load();
    }

    @Override
    public void save(ResultsSnapshot results) throws PersistenceException {
        store.save(results);
    }
}
