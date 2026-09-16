package dev.konqasasas.beat.roster;

import java.nio.file.Path;
import java.util.List;

public interface IdentityFileLoader {
    List<RegisteredIdentity> load(Path file) throws RosterException;
}
