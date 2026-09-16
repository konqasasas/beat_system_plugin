package dev.konqasasas.beat.application;

import dev.konqasasas.beat.domain.CompetitionKind;
import dev.konqasasas.beat.domain.high.HighCompetitionSession;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.HighResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HighResultService {
    private final ResultsRepository repository;

    public HighResultService(ResultsRepository repository) {
        this.repository = repository;
    }

    public ResultsSnapshot saveFinal(HighCompetitionSession session) throws PersistenceException {
        ResultsSnapshot current = repository.load().orElse(new ResultsSnapshot(
                ResultsSnapshot.CURRENT_SCHEMA_VERSION, false, false, false, false, List.of()));
        Map<UUID, PlayerResultSnapshot> previous = new LinkedHashMap<>();
        current.players().forEach(player -> previous.put(player.uuid(), player));
        List<PlayerResultSnapshot> players = new ArrayList<>();
        session.rankings().forEach(entry -> {
            UUID uuid = entry.competitor().uuid();
            PlayerResultSnapshot old = previous.remove(uuid);
            players.add(new PlayerResultSnapshot(
                    uuid,
                    entry.competitor().tournamentName(),
                    entry.competitor().participatedIn(CompetitionKind.HIGH_DIFFICULTY),
                    old != null && old.participatedTa(),
                    old != null && old.participatedEndurance(),
                    old != null && old.overallExcluded(),
                    old != null && old.disqualified(),
                    new HighResultSnapshot(entry.rank(), entry.record().points(),
                            entry.record().finalPointTick().isPresent()
                                    ? entry.record().finalPointTick().getAsLong() : null),
                    old == null ? null : old.timeAttack(),
                    old == null ? null : old.endurance(),
                    old == null ? null : old.overall()));
        });
        players.addAll(previous.values());
        ResultsSnapshot saved = new ResultsSnapshot(
                current.schemaVersion(), true, current.timeAttackConfirmed(), current.enduranceConfirmed(),
                false, players);
        repository.save(saved);
        return saved;
    }

    public ResultsSnapshot current() throws PersistenceException {
        return repository.load().orElse(new ResultsSnapshot(
                ResultsSnapshot.CURRENT_SCHEMA_VERSION, false, false, false, false, List.of()));
    }

    public void clearHigh() throws PersistenceException {
        ResultsSnapshot current = current();
        List<PlayerResultSnapshot> players = current.players().stream().map(old -> new PlayerResultSnapshot(
                old.uuid(), old.tournamentName(), false, old.participatedTa(), old.participatedEndurance(),
                old.overallExcluded(), old.disqualified(), null, old.timeAttack(), old.endurance(), null)).toList();
        repository.save(new ResultsSnapshot(
                current.schemaVersion(), false, current.timeAttackConfirmed(), current.enduranceConfirmed(),
                false, players));
    }
}
