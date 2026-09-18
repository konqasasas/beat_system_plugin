package dev.konqasasas.beat.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable world/chunk index for endurance progress locations. */
public final class EnduranceProgressIndex {
    private final Map<ChunkKey, List<Entry>> byChunk;
    private final List<Entry> entries;
    private final List<Integer> progressNumbers;

    public EnduranceProgressIndex(Map<Integer, List<EnduranceProgressPoint>> progresses) {
        Objects.requireNonNull(progresses, "progresses");
        List<Entry> all = new ArrayList<>();
        Map<ChunkKey, List<Entry>> chunks = new LinkedHashMap<>();
        progresses.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(progress -> {
            for (int index = 0; index < progress.getValue().size(); index++) {
                EnduranceProgressPoint point = progress.getValue().get(index);
                Entry entry = new Entry(progress.getKey(), index, point);
                all.add(entry);
                chunks.computeIfAbsent(entry.chunk(), ignored -> new ArrayList<>()).add(entry);
            }
        });
        entries = List.copyOf(all);
        Map<ChunkKey, List<Entry>> immutable = new LinkedHashMap<>();
        chunks.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        byChunk = Map.copyOf(immutable);
        progressNumbers = progresses.keySet().stream().sorted().toList();
    }

    public List<Entry> entries() {
        return entries;
    }

    public List<Entry> inChunk(String world, int chunkX, int chunkZ) {
        return byChunk.getOrDefault(new ChunkKey(world, chunkX, chunkZ), List.of());
    }

    public List<Entry> near(MapLocation location, double horizontalRadius) {
        Objects.requireNonNull(location, "location");
        int minChunkX = chunkCoordinate(location.x() - horizontalRadius);
        int maxChunkX = chunkCoordinate(location.x() + horizontalRadius);
        int minChunkZ = chunkCoordinate(location.z() - horizontalRadius);
        int maxChunkZ = chunkCoordinate(location.z() + horizontalRadius);
        List<Entry> result = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                result.addAll(inChunk(location.world(), chunkX, chunkZ));
            }
        }
        return List.copyOf(result);
    }

    public int highestContaining(MapLocation location, double horizontalRadius, double verticalTolerance) {
        Objects.requireNonNull(location, "location");
        int minChunkX = chunkCoordinate(location.x() - horizontalRadius);
        int maxChunkX = chunkCoordinate(location.x() + horizontalRadius);
        int minChunkZ = chunkCoordinate(location.z() - horizontalRadius);
        int maxChunkZ = chunkCoordinate(location.z() + horizontalRadius);
        int highest = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (Entry entry : inChunk(location.world(), chunkX, chunkZ)) {
                    if (entry.progress() > highest
                            && entry.point().contains(location, horizontalRadius, verticalTolerance)) {
                        highest = entry.progress();
                    }
                }
            }
        }
        return highest;
    }

    public Integer nextProgress(int currentProgress) {
        return progressNumbers.stream().filter(number -> number > currentProgress).findFirst().orElse(null);
    }

    public static int chunkCoordinate(double coordinate) {
        return ((int) Math.floor(coordinate)) >> 4;
    }

    public record ChunkKey(String world, int x, int z) {
        public ChunkKey {
            Objects.requireNonNull(world, "world");
        }
    }

    public record Entry(int progress, int locationIndex, EnduranceProgressPoint point) {
        public Entry {
            if (progress < 1 || locationIndex < 0) throw new IllegalArgumentException("Invalid progress entry");
            Objects.requireNonNull(point, "point");
        }

        public ChunkKey chunk() {
            return new ChunkKey(point.world(), chunkCoordinate(point.x()), chunkCoordinate(point.z()));
        }
    }
}
