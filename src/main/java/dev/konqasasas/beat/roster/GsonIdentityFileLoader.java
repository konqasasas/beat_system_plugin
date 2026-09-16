package dev.konqasasas.beat.roster;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.konqasasas.beat.persistence.JsonSupport;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class GsonIdentityFileLoader implements IdentityFileLoader {
    private static final Type ENTRY_LIST_TYPE = TypeToken
            .getParameterized(List.class, IdentityJson.class)
            .getType();

    private final Gson gson = JsonSupport.createGson();

    @Override
    public List<RegisteredIdentity> load(Path file) throws RosterException {
        if (!Files.isRegularFile(file)) {
            throw new RosterException("Identity file does not exist: " + file);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<IdentityJson> decoded = gson.fromJson(reader, ENTRY_LIST_TYPE);
            if (decoded == null) {
                throw new IllegalArgumentException("Identity list must not be null");
            }
            List<RegisteredIdentity> identities = new ArrayList<>(decoded.size());
            for (IdentityJson entry : decoded) {
                if (entry == null || entry.uuid == null || entry.mcid == null) {
                    throw new IllegalArgumentException("Identity entries require uuid and mcid");
                }
                identities.add(new RegisteredIdentity(parseUuid(entry.uuid), entry.mcid));
            }
            return List.copyOf(identities);
        } catch (IOException | RuntimeException exception) {
            throw new RosterException("Failed to load identity file: " + file, exception);
        }
    }

    private static UUID parseUuid(String configured) {
        String value = configured.trim();
        if (value.matches("[0-9a-fA-F]{32}")) {
            value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-"
                    + value.substring(12, 16) + "-" + value.substring(16, 20) + "-"
                    + value.substring(20);
        }
        return UUID.fromString(value);
    }

    private record IdentityJson(String uuid, String mcid) {
    }
}
