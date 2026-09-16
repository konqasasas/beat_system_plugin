package dev.konqasasas.beat.roster;

import java.nio.file.Path;
import java.util.Objects;

public final class RosterService {
    private final IdentityFileLoader loader;
    private final Path participantsFile;
    private final Path adminsFile;
    private volatile RosterSnapshot current;

    public RosterService(
            IdentityFileLoader loader,
            Path participantsFile,
            Path adminsFile) throws RosterException {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.participantsFile = Objects.requireNonNull(participantsFile, "participantsFile");
        this.adminsFile = Objects.requireNonNull(adminsFile, "adminsFile");
        reload();
    }

    public RosterSnapshot current() {
        return current;
    }

    public synchronized void reload() throws RosterException {
        var candidateParticipants = loader.load(participantsFile);
        var candidateAdmins = loader.load(adminsFile);
        RosterSnapshot candidate = RosterSnapshot.create(candidateParticipants, candidateAdmins);
        current = candidate;
    }
}
