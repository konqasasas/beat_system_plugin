package dev.konqasasas.beat.application;

import dev.konqasasas.beat.domain.ranking.OverallRanking;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.OverallResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OverallService {
    private final ResultsRepository results;
    private final EventStateService states;

    public OverallService(ResultsRepository results, EventStateService states) {
        this.results = results;
        this.states = states;
    }

    public ResultsSnapshot calculate() throws PersistenceException {
        ResultsSnapshot current = current();
        if (states.current().tournamentState() != TournamentState.ENDURANCE_FINISHED
                && states.current().tournamentState() != TournamentState.OVERALL_READY) {
            throw new IllegalStateException("耐久終了後に総合順位を計算してください");
        }
        if (current.overallConfirmed()) throw new IllegalStateException("総合結果は確定済みです");
        if (!current.highConfirmed() || !current.timeAttackConfirmed() || !current.enduranceConfirmed()) {
            throw new IllegalStateException("3競技すべての結果確定が必要です");
        }
        Map<UUID, OverallRanking.Standing> standings = new LinkedHashMap<>();
        OverallRanking.calculate(current.players()).forEach(s -> standings.put(s.player().uuid(), s));
        List<PlayerResultSnapshot> players = current.players().stream().map(player -> {
            OverallRanking.Standing standing = standings.get(player.uuid());
            OverallResultSnapshot overall = standing == null ? null : new OverallResultSnapshot(
                    standing.rank(), standing.scoreProduct(), player.high().rank(),
                    player.timeAttack().rank(), player.endurance().rank(), false);
            return copy(player, player.overallExcluded(), player.disqualified(), overall);
        }).toList();
        ResultsSnapshot saved = new ResultsSnapshot(current.schemaVersion(), true, true, true, false, players);
        results.save(saved);
        if (states.current().tournamentState() == TournamentState.ENDURANCE_FINISHED) {
            states.transitionTo(TournamentState.OVERALL_READY);
        }
        return saved;
    }

    public ResultsSnapshot confirm() throws PersistenceException {
        ResultsSnapshot current = current();
        if (current.overallConfirmed()) throw new IllegalStateException("総合結果は既に確定済みです");
        if (current.players().stream().filter(OverallService::eligible).anyMatch(p -> p.overall() == null)) {
            throw new IllegalStateException("先に /beat overall calculate を実行してください");
        }
        List<PlayerResultSnapshot> players = current.players().stream().map(player -> {
            OverallResultSnapshot o = player.overall();
            OverallResultSnapshot confirmed = o == null ? null : new OverallResultSnapshot(
                    o.rank(), o.scoreProduct(), o.highRank(), o.taRank(), o.enduranceRank(), true);
            return copy(player, player.overallExcluded(), player.disqualified(), confirmed);
        }).toList();
        ResultsSnapshot saved = new ResultsSnapshot(current.schemaVersion(), current.highConfirmed(),
                current.timeAttackConfirmed(), current.enduranceConfirmed(), true, players);
        results.save(saved);
        states.transitionTo(TournamentState.OVERALL_CONFIRMED);
        return saved;
    }

    public ResultsSnapshot setPlayerFlag(String name, Boolean excluded, Boolean disqualified)
            throws PersistenceException {
        ResultsSnapshot current = current();
        if (current.overallConfirmed()) throw new IllegalStateException("総合結果は確定済みです");
        boolean found = current.players().stream().anyMatch(p -> p.tournamentName().equalsIgnoreCase(name));
        if (!found) throw new IllegalArgumentException("結果にプレイヤーが見つかりません: " + name);
        List<PlayerResultSnapshot> players = current.players().stream().map(player ->
                player.tournamentName().equalsIgnoreCase(name)
                        ? copy(player, excluded == null ? player.overallExcluded() : excluded,
                                disqualified == null ? player.disqualified() : disqualified, null)
                        : copy(player, player.overallExcluded(), player.disqualified(), null)).toList();
        ResultsSnapshot saved = new ResultsSnapshot(current.schemaVersion(), current.highConfirmed(),
                current.timeAttackConfirmed(), current.enduranceConfirmed(), false, players);
        results.save(saved);
        return saved;
    }

    public ResultsSnapshot current() throws PersistenceException {
        return results.load().orElse(new ResultsSnapshot(1, false, false, false, false, List.of()));
    }

    private static boolean eligible(PlayerResultSnapshot p) {
        return p.participatedHigh() && p.participatedTa() && p.participatedEndurance()
                && !p.overallExcluded() && !p.disqualified();
    }
    private static PlayerResultSnapshot copy(PlayerResultSnapshot p, boolean excluded, boolean disqualified,
            OverallResultSnapshot overall) {
        return new PlayerResultSnapshot(p.uuid(), p.tournamentName(), p.participatedHigh(), p.participatedTa(),
                p.participatedEndurance(), excluded, disqualified, p.high(), p.timeAttack(), p.endurance(), overall);
    }
}
