package dev.konqasasas.beat.domain.ta;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.CompetitionKind;
import dev.konqasasas.beat.domain.ranking.CompetitionRankings;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.ranking.RankingEntry;
import java.util.*;

public final class TimeAttackSession {
    private final Map<UUID, State> players = new LinkedHashMap<>();
    private long sequence;
    private List<RankedEntry<TimeAttackRecord>> cachedRankings;

    public TimeAttackSession(Map<UUID, String> names) {
        names.forEach((id, name) -> players.put(id, new State(new Competitor(id, name))));
    }
    public void activate(UUID id) { State s=require(id); if (!s.record.frozen()) { s.active=true; s.competitor.markParticipated(CompetitionKind.TIME_ATTACK); } }
    public boolean enterStart(UUID id, boolean wasInside, boolean isInside, boolean playerMovement, long tick) {
        State s=requireActive(id);
        if (!playerMovement || wasInside || !isInside) return false;
        s.startTick=tick; s.running=true; s.splits.clear(); s.displayReset=false; return true;
    }
    public OptionalLong reachSplit(UUID id, int number, long tick) {
        State s=requireActive(id); if (!s.running) return OptionalLong.empty();
        long elapsed=tick-s.startTick; return s.splits.putIfAbsent(number, elapsed)==null ? OptionalLong.of(elapsed) : OptionalLong.empty();
    }
    public GoalResult reachGoal(UUID id, long tick) {
        State s=requireActive(id); if (!s.running) return GoalResult.invalid();
        int oldRank=rank(id); long elapsed=tick-s.startTick;
        var update=s.record.recordGoal(elapsed,tick,++sequence,s.splits.get(1),s.splits.get(2));
        s.running=false; s.displayReset=true;
        if (update.updated()) cachedRankings=null;
        int newRank=rank(id); return new GoalResult(true,elapsed,update.updated(),oldRank,newRank,update.previousBestTicks(),update.currentBestTicks());
    }
    public void markRestarted(UUID id) { requireActive(id).displayReset=true; }
    public long displayedElapsed(UUID id,long tick) { State s=require(id); return s.running&&!s.displayReset ? tick-s.startTick : 0; }
    public boolean running(UUID id){return require(id).running;}
    public List<UUID> eliminateToTop(int count) {
        Set<UUID> keep=new LinkedHashSet<>();
        for(var e:rankings()) if(keep.size()<count && require(e.competitor().uuid()).active
                && !e.competitor().disqualified() && e.record().hasPersonalBest()) keep.add(e.competitor().uuid());
        List<UUID> out=new ArrayList<>();
        players.forEach((id,s)->{if(s.active&&!keep.contains(id)){s.active=false;s.running=false;s.record.freeze();out.add(id);}});
        return List.copyOf(out);
    }
    public void finish(){players.values().forEach(s->{s.active=false;s.running=false;s.record.freeze();});}
    public List<RankedEntry<TimeAttackRecord>> rankings(){if(cachedRankings==null)cachedRankings=CompetitionRankings.timeAttack(players.values().stream().map(s->new RankingEntry<>(s.competitor,s.record)).toList());return cachedRankings;}
    public int rank(UUID id){return rankings().stream().filter(e->e.competitor().uuid().equals(id)).findFirst().orElseThrow().rank();}
    public TimeAttackRecord record(UUID id){return require(id).record;}
    public Competitor competitor(UUID id){return require(id).competitor;}
    public boolean active(UUID id){return require(id).active;}
    public boolean contains(UUID id){return players.containsKey(id);}
    private State requireActive(UUID id){State s=require(id);if(!s.active||s.record.frozen())throw new IllegalStateException("player is not active");return s;}
    private State require(UUID id){State s=players.get(id);if(s==null)throw new IllegalArgumentException("unknown player");return s;}
    public record GoalResult(boolean valid,long elapsedTicks,boolean personalBest,int previousRank,int currentRank,Long previousBest,Long currentBest){static GoalResult invalid(){return new GoalResult(false,0,false,0,0,null,null);} public boolean rankChanged(){return personalBest&&previousRank!=currentRank;}}
    private static final class State{final Competitor competitor;final TimeAttackRecord record=new TimeAttackRecord();boolean active,running,displayReset;long startTick;Map<Integer,Long> splits=new HashMap<>();State(Competitor c){competitor=c;}}
}
