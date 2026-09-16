package dev.konqasasas.beat.map.validation;

@FunctionalInterface
public interface WorldRegistry {
    boolean worldExists(String worldName);
}
