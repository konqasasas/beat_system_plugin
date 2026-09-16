package dev.konqasasas.beat.map.persistence;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

final class AtomicYamlFileStore<T> {
    private final Path target;
    private final Class<T> type;
    private final Gson converter;
    private final Yaml loader;
    private final Yaml dumper;

    AtomicYamlFileStore(Path target, Class<T> type) {
        this.target = Objects.requireNonNull(target, "target").toAbsolutePath().normalize();
        this.type = Objects.requireNonNull(type, "type");
        converter = new GsonBuilder()
                .serializeNulls()
                .setStrictness(Strictness.STRICT)
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                .disableJdkUnsafe()
                .create();

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setMaxAliasesForCollections(50);
        loaderOptions.setCodePointLimit(2_000_000);
        loader = new Yaml(new SafeConstructor(loaderOptions));

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(2);
        dumper = new Yaml(dumperOptions);
    }

    synchronized Optional<T> load() throws PersistenceException {
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
            Object yamlData = loader.load(reader);
            if (yamlData == null) {
                throw new IllegalArgumentException("YAML document must not be empty");
            }
            T value = converter.fromJson(converter.toJson(yamlData), type);
            if (value == null) {
                throw new IllegalArgumentException("YAML document must not decode to null");
            }
            return Optional.of(value);
        } catch (IOException | RuntimeException exception) {
            throw new PersistenceException("Failed to load YAML from " + target, exception);
        }
    }

    synchronized void save(T value) throws PersistenceException {
        Objects.requireNonNull(value, "value");
        Path parent = target.getParent();
        Path temporary = null;
        try {
            Files.createDirectories(parent);
            temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
            Object plainData = toPlainData(converter.toJsonTree(value));
            try (FileChannel channel = FileChannel.open(
                            temporary,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    Writer writer = Channels.newWriter(channel, StandardCharsets.UTF_8)) {
                dumper.dump(plainData, writer);
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
            throw new PersistenceException("Failed to save YAML to " + target, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // The original map remains intact; a stale temporary file is harmless.
                }
            }
        }
    }

    private static Object toPlainData(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        }
        if (element.isJsonObject()) {
            Map<String, Object> result = new LinkedHashMap<>();
            element.getAsJsonObject().entrySet().forEach(entry ->
                    result.put(entry.getKey(), toPlainData(entry.getValue())));
            return result;
        }
        if (element.isJsonArray()) {
            List<Object> result = new ArrayList<>();
            element.getAsJsonArray().forEach(item -> result.add(toPlainData(item)));
            return result;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isString()) {
            return primitive.getAsString();
        }
        String number = primitive.getAsString();
        if (number.contains(".") || number.contains("e") || number.contains("E")) {
            return new BigDecimal(number);
        }
        return Long.parseLong(number);
    }
}
