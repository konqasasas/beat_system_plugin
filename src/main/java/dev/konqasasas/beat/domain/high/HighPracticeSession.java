package dev.konqasasas.beat.domain.high;

import dev.konqasasas.beat.map.MapLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class HighPracticeSession {
    public enum Phase { COUNTDOWN, PRACTICE, PREPARE, COMPLETE }

    private final long countdownTicks;
    private final long practiceTicks;
    private final long prepareTicks;
    private final Map<UUID, PracticePlayerState> players = new HashMap<>();
    private Phase phase = Phase.COUNTDOWN;
    private long phaseTick;

    public HighPracticeSession(Set<UUID> participants, long countdownTicks, long practiceTicks, long prepareTicks) {
        Objects.requireNonNull(participants, "participants");
        if (countdownTicks < 1 || practiceTicks < 1 || prepareTicks < 1) {
            throw new IllegalArgumentException("phase durations must be positive");
        }
        this.countdownTicks = countdownTicks;
        this.practiceTicks = practiceTicks;
        this.prepareTicks = prepareTicks;
        participants.forEach(uuid -> players.put(Objects.requireNonNull(uuid, "participant"), new PracticePlayerState()));
    }

    public Phase advanceOneTick() {
        if (phase == Phase.COMPLETE) throw new IllegalStateException("practice session is complete");
        phaseTick++;
        long duration = switch (phase) {
            case COUNTDOWN -> countdownTicks;
            case PRACTICE -> practiceTicks;
            case PREPARE -> prepareTicks;
            case COMPLETE -> throw new IllegalStateException();
        };
        if (phaseTick >= duration) {
            phase = switch (phase) {
                case COUNTDOWN -> Phase.PRACTICE;
                case PRACTICE -> Phase.PREPARE;
                case PREPARE -> Phase.COMPLETE;
                case COMPLETE -> throw new IllegalStateException();
            };
            phaseTick = 0;
            if (phase == Phase.PREPARE) players.values().forEach(PracticePlayerState::clear);
        }
        return phase;
    }

    public boolean toggleFlight(UUID playerId) {
        requirePractice();
        PracticePlayerState state = requirePlayer(playerId);
        state.flightEnabled = !state.flightEnabled;
        return state.flightEnabled;
    }

    public boolean setCheckpoint(UUID playerId, MapLocation location, boolean onGround) {
        requirePractice();
        if (!onGround) return false;
        requirePlayer(playerId).checkpoint = Objects.requireNonNull(location, "location");
        return true;
    }

    public Optional<MapLocation> checkpoint(UUID playerId) {
        return Optional.ofNullable(requirePlayer(playerId).checkpoint);
    }

    public boolean flightEnabled(UUID playerId) {
        return requirePlayer(playerId).flightEnabled;
    }

    public void skipPractice() {
        requirePractice();
        phase = Phase.PREPARE;
        phaseTick = 0;
        players.values().forEach(PracticePlayerState::clear);
    }

    public boolean contains(UUID playerId) { return players.containsKey(playerId); }
    public Phase phase() { return phase; }
    public long phaseTick() { return phaseTick; }
    public long phaseDurationTicks() {
        return switch (phase) {
            case COUNTDOWN -> countdownTicks;
            case PRACTICE -> practiceTicks;
            case PREPARE -> prepareTicks;
            case COMPLETE -> 0;
        };
    }
    public long remainingTicks() {
        long duration = switch (phase) {
            case COUNTDOWN -> countdownTicks;
            case PRACTICE -> practiceTicks;
            case PREPARE -> prepareTicks;
            case COMPLETE -> 0;
        };
        return Math.max(0, duration - phaseTick);
    }

    private void requirePractice() {
        if (phase != Phase.PRACTICE) throw new IllegalStateException("practice is not active");
    }

    private PracticePlayerState requirePlayer(UUID playerId) {
        PracticePlayerState state = players.get(playerId);
        if (state == null) throw new IllegalArgumentException("player is not in this practice session");
        return state;
    }

    private static final class PracticePlayerState {
        private boolean flightEnabled;
        private MapLocation checkpoint;
        private void clear() { flightEnabled = false; checkpoint = null; }
    }
}
