package dev.konqasasas.beat.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FileBackupService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
            .ofPattern("uuuuMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC);

    private final Path backupDirectory;
    private final Clock clock;

    public FileBackupService(Path backupDirectory) {
        this(backupDirectory, Clock.systemUTC());
    }

    public FileBackupService(Path backupDirectory, Clock clock) {
        this.backupDirectory = Objects.requireNonNull(backupDirectory, "backupDirectory")
                .toAbsolutePath()
                .normalize();
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public List<Path> backupExisting(List<Path> sourceFiles, String reason)
            throws PersistenceException {
        String safeReason = requireSafeReason(reason);
        String timestamp = TIMESTAMP.format(Instant.now(clock));
        List<Path> created = new ArrayList<>();
        try {
            Files.createDirectories(backupDirectory);
            for (Path sourceFile : sourceFiles) {
                Path normalizedSource = sourceFile.toAbsolutePath().normalize();
                if (!Files.isRegularFile(normalizedSource)) {
                    continue;
                }
                String backupName = timestamp + "-" + safeReason + "-" + normalizedSource.getFileName();
                Path target = uniqueTarget(backupName);
                Files.copy(normalizedSource, target, StandardCopyOption.COPY_ATTRIBUTES);
                created.add(target);
            }
            return List.copyOf(created);
        } catch (IOException exception) {
            throw new PersistenceException(
                    "Failed to create backup in " + backupDirectory, exception);
        }
    }

    private Path uniqueTarget(String fileName) {
        Path candidate = backupDirectory.resolve(fileName);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = backupDirectory.resolve(fileName + "." + suffix);
            suffix++;
        }
        return candidate;
    }

    private static String requireSafeReason(String reason) {
        Objects.requireNonNull(reason, "reason");
        if (!reason.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("reason must match [a-z0-9-]+");
        }
        return reason;
    }
}
