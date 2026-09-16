package dev.konqasasas.beat.application;

import dev.konqasasas.beat.persistence.PersistenceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public final class ResultAuditLog {
    private final Path file;
    public ResultAuditLog(Path file) { this.file = file; }
    public synchronized void write(String admin, String competition, String player,
            String field, String before, String after) throws PersistenceException {
        String entry = "%s%nAdmin: %s%nCompetition: %s%nPlayer: %s%nField: %s%nBefore: %s%nAfter: %s%n%n"
                .formatted(OffsetDateTime.now(), admin, competition, player, field, before, after);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new PersistenceException("結果編集ログを書き込めません", exception);
        }
    }
}
