package dev.konqasasas.beat.persistence;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;

final class AtomicJsonFileStore<T> {
    private final Path target;
    private final Class<T> type;
    private final Gson gson;

    AtomicJsonFileStore(Path target, Class<T> type, Gson gson) {
        this.target = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        this.type = Objects.requireNonNull(type, "type");
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    synchronized Optional<T> load() throws PersistenceException {
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            T value = gson.fromJson(reader, type);
            if (value == null) {
                throw new IllegalArgumentException("JSON document must not be null");
            }
            return Optional.of(value);
        } catch (IOException | RuntimeException exception) {
            throw new PersistenceException("Failed to load JSON from " + target, exception);
        }
    }

    synchronized void save(T value) throws PersistenceException {
        Objects.requireNonNull(value, "value");
        Path parent = target.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
            try (FileChannel channel = FileChannel.open(
                            temporary,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {
                gson.toJson(value, writer);
                writer.flush();
                channel.force(true);
            }
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new PersistenceException(
                        "Atomic file replacement is not supported for " + target, exception);
            }
            temporary = null;
        } catch (IOException | RuntimeException exception) {
            throw new PersistenceException("Failed to save JSON to " + target, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The original data is intact. A stale temporary file is safe to ignore.
                }
            }
        }
    }

    Path target() {
        return target;
    }
}
