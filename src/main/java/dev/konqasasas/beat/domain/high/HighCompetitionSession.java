package dev.konqasasas.beat.domain.high;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.CompetitionKind;
import dev.konqasasas.beat.domain.ranking.CompetitionRankings;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ranking.RankingEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class HighCompetitionSession {
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();

    public HighCompetitionSession(Map<UUID, String> participantNames, HighDifficultyRules rules) {
        Objects.requireNonNull(participantNames, "participantNames");
        Objects.requireNonNull(rules, "rules");
        participantNames.forEach((uuid, name) -> players.put(
                Objects.requireNonNull(uuid, "uuid"),
                new PlayerState(new Competitor(uuid, name), new HighDifficultyRecord(rules))));
    }

    public void activate(UUID playerId) {
        PlayerState state = requirePlayer(playerId);
        if (!state.record.frozen()) {
            state.active = true;
            state.competitor.markParticipated(CompetitionKind.HIGH_DIFFICULTY);
        }
    }

    public ScoreResult reachSpot(UUID playerId, int course, int spot, long tick) {
        PlayerState state = requireActive(playerId);
        int previousRank = rank(playerId);
        HighDifficultyRecord.ScoreUpdate score = state.record.reachSpot(course, spot, tick);
        int currentRank = rank(playerId);
        return new ScoreResult(score, previousRank, currentRank, false);
    }

    public ScoreResult reachGoal(UUID playerId, int course, long tick) {
        PlayerState state = requireActive(playerId);
        int previousRank = rank(playerId);
        HighDifficultyRecord.ScoreUpdate score = state.record.reachGoal(course, tick);
        if (score.changed()) state.currentCourse = Math.max(state.currentCourse, Math.min(5, course + 1));
        int currentRank = rank(playerId);
        return new ScoreResult(
                score, previousRank, currentRank,
                score.changed() && state.record.allCoursesCleared());
    }

    public List<Elimination> eliminateBelowCourse(int requiredCourse) {
        if (requiredCourse < 2 || requiredCourse > 5) {
            throw new IllegalArgumentException("requiredCourse must be 2..5");
        }
        List<Elimination> eliminated = new ArrayList<>();
        for (var entry : rankings()) {
            PlayerState state = players.get(entry.competitor().uuid());
            if (state.active && !state.record.frozen() && state.currentCourse < requiredCourse) {
                state.record.freeze();
                state.active = false;
                state.eliminated = true;
                eliminated.add(new Elimination(entry.competitor().uuid(), entry.rank(), state.record.points()));
            }
        }
        return List.copyOf(eliminated);
    }

    public void finish() {
        players.values().forEach(state -> {
            state.record.freeze();
            state.active = false;
        });
    }

    public List<RankedEntry<HighDifficultyRecord>> rankings() {
        return CompetitionRankings.highDifficulty(players.values().stream()
                .map(state -> new RankingEntry<>(state.competitor, state.record)).toList());
    }

    public int rank(UUID playerId) {
        return rankings().stream().filter(entry -> entry.competitor().uuid().equals(playerId))
                .findFirst().orElseThrow().rank();
    }

    public HighDifficultyRecord record(UUID playerId) { return requirePlayer(playerId).record; }
    public Competitor competitor(UUID playerId) { return requirePlayer(playerId).competitor; }
    public int currentCourse(UUID playerId) { return requirePlayer(playerId).currentCourse; }
    public boolean active(UUID playerId) { return requirePlayer(playerId).active; }
    public boolean eliminated(UUID playerId) { return requirePlayer(playerId).eliminated; }
    public boolean contains(UUID playerId) { return players.containsKey(playerId); }

    private PlayerState requireActive(UUID playerId) {
        PlayerState state = requirePlayer(playerId);
        if (!state.active || state.record.frozen()) throw new IllegalStateException("player is not active");
        return state;
    }

    private PlayerState requirePlayer(UUID playerId) {
        PlayerState state = players.get(playerId);
        if (state == null) throw new IllegalArgumentException("unknown player");
        return state;
    }

    public record ScoreResult(
            HighDifficultyRecord.ScoreUpdate score, int previousRank, int currentRank, boolean allClear) {
        public boolean rankChanged() { return score.changed() && previousRank != currentRank; }
    }
    public record Elimination(UUID playerId, int rank, int points) {}

    private static final class PlayerState {
        private final Competitor competitor;
        private final HighDifficultyRecord record;
        private boolean active;
        private boolean eliminated;
        private int currentCourse = 1;
        private PlayerState(Competitor competitor, HighDifficultyRecord record) {
            this.competitor = competitor;
            this.record = record;
        }
    }
}
