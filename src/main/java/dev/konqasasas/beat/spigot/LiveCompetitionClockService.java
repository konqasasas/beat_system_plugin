package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.List;

public final class LiveCompetitionClockService {
    private final List<LiveCompetitionClock> clocks;

    public LiveCompetitionClockService(LiveCompetitionClock... clocks) { this.clocks = List.of(clocks); }

    public LiveCompetitionClock.Snapshot status() { return active().snapshot; }

    public LiveCompetitionClock.Snapshot set(long target) throws PersistenceException {
        Active active = active();
        if (target < active.snapshot.elapsedTick()) throw new IllegalArgumentException("競技時間は巻き戻せません");
        if (target >= active.snapshot.totalTicks()) throw new IllegalArgumentException("終了時刻より前を指定してください");
        active.clock.debugSetElapsedTick(target);
        return active.clock.liveClock().orElseThrow(() -> new IllegalStateException("競技が終了しました"));
    }

    public LiveCompetitionClock.Snapshot advance(long ticks) throws PersistenceException {
        if (ticks < 0) throw new IllegalArgumentException("進める時間は0以上です");
        LiveCompetitionClock.Snapshot current = status();
        return set(Math.min(current.totalTicks() - 1, current.elapsedTick() + ticks));
    }

    public LiveCompetitionClock.Snapshot next() throws PersistenceException {
        LiveCompetitionClock.Snapshot current = status();
        return set(current.nextTarget());
    }

    private Active active() {
        for (LiveCompetitionClock clock : clocks) {
            var snapshot = clock.liveClock();
            if (snapshot.isPresent()) return new Active(clock, snapshot.get());
        }
        throw new IllegalStateException("進行中の競技フェーズがありません");
    }

    private record Active(LiveCompetitionClock clock, LiveCompetitionClock.Snapshot snapshot) { }
}
