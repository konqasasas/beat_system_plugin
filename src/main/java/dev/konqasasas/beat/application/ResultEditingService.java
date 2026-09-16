package dev.konqasasas.beat.application;

import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.ResultsRepository;
import dev.konqasasas.beat.persistence.snapshot.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

public final class ResultEditingService {
    public enum Competition { HIGH, TA, ENDURANCE }
    private final ResultsRepository repository;
    private final MapConfigurationService maps;
    private final ResultAuditLog audit;
    private ResultsSnapshot undo;

    public ResultEditingService(ResultsRepository repository, MapConfigurationService maps, ResultAuditLog audit) {
        this.repository = repository; this.maps = maps; this.audit = audit;
    }

    public synchronized void edit(String admin, Competition competition, String playerName,
            String field, String value, Integer split) throws PersistenceException {
        ResultsSnapshot current = current();
        if (current.overallConfirmed()) throw new IllegalStateException("総合結果確定後は編集できません");
        PlayerResultSnapshot player = find(current, playerName);
        PlayerResultSnapshot changed = switch (competition) {
            case HIGH -> editHigh(player, field, value);
            case TA -> editTa(player, field, value, split, current);
            case ENDURANCE -> editEndurance(player, field, value);
        };
        List<PlayerResultSnapshot> players = current.players().stream()
                .map(p -> p.uuid().equals(player.uuid()) ? changed : clearOverall(p)).toList();
        ResultsSnapshot candidate = flags(current, competition, false, players);
        repository.save(candidate);
        undo = current;
        audit.write(admin, competition.name(), player.tournamentName(), field,
                describe(player, competition, field, split), value);
    }

    public synchronized ResultsSnapshot recalculate(Competition competition) throws PersistenceException {
        ResultsSnapshot current = current();
        List<PlayerResultSnapshot> players = switch (competition) {
            case HIGH -> rank(current.players(), p -> p.high(), highComparator(),
                    (p, rank) -> withHigh(p, new HighResultSnapshot(rank, p.high().points(), p.high().finalPointTick())));
            case TA -> rank(current.players(), p -> p.timeAttack(), taComparator(),
                    (p, rank) -> withTa(p, copyTa(p.timeAttack(), rank)));
            case ENDURANCE -> rank(current.players(), p -> p.endurance(), enduranceComparator(),
                    (p, rank) -> withEndurance(p, copyEndurance(p.endurance(), rank)));
        };
        ResultsSnapshot saved = flags(current, competition, false, players.stream().map(ResultEditingService::clearOverall).toList());
        repository.save(saved); return saved;
    }

    public synchronized ResultsSnapshot confirm(String admin, Competition competition) throws PersistenceException {
        ResultsSnapshot current = current();
        if (current.players().stream().map(extractor(competition)).filter(Objects::nonNull).anyMatch(r -> rankOf(r) == null))
            throw new IllegalStateException("先にrecalculateを実行してください");
        ResultsSnapshot saved = flags(current, competition, true, current.players());
        repository.save(saved);
        audit.write(admin, competition.name(), "-", "confirm", "false", "true");
        return saved;
    }

    public synchronized void undo(String admin) throws PersistenceException {
        if (undo == null) throw new IllegalStateException("Undoできる編集がありません");
        ResultsSnapshot restore = undo; undo = null; repository.save(restore);
        audit.write(admin, "UNDO", "-", "last-edit", "edited", "restored");
    }

    public synchronized void clearUndoHistory() {
        undo = null;
    }

    public ResultsSnapshot current() throws PersistenceException {
        return repository.load().orElseThrow(() -> new PersistenceException("results.jsonがありません"));
    }

    public static long parseTimeTicks(String value) {
        try {
            BigDecimal hundredths = new BigDecimal(value).movePointRight(2);
            int units = hundredths.intValueExact();
            if (units < 0 || units % 5 != 0) throw new ArithmeticException();
            return units / 5L;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("タイムは0.05秒単位で指定してください: " + value);
        }
    }

    private PlayerResultSnapshot editHigh(PlayerResultSnapshot p, String field, String value) {
        HighResultSnapshot old = require(p.high(), "高難易度記録");
        int points = field.equals("points") ? nonnegativeInt(value) : old.points();
        Long tick = field.equals("reached-tick") ? nonnegativeLong(value) : old.finalPointTick();
        if (!field.equals("points") && !field.equals("reached-tick")) throw new IllegalArgumentException("fieldはpoints/reached-tickです");
        if (points > 0 && tick == null) tick = 0L;
        return withHigh(p, new HighResultSnapshot(null, points, tick));
    }

    private PlayerResultSnapshot editTa(PlayerResultSnapshot p, String field, String value, Integer split, ResultsSnapshot all) {
        TimeAttackResultSnapshot old = require(p.timeAttack(), "TA記録");
        Long time=old.pbTicks(), reached=old.pbRecordedTick(), sequence=old.pbRecordSequence(), s1=old.pbSplit1Ticks(), s2=old.pbSplit2Ticks();
        if (field.equals("time")) {
            time=value.equalsIgnoreCase("none")?null:parseTimeTicks(value);
            if(time==null){reached=null;sequence=null;s1=null;s2=null;}else{if(reached==null)reached=0L;if(sequence==null)sequence=nextSequence(all);}
        } else if(field.equals("reached-tick")) reached=nonnegativeLong(value);
        else if(field.equals("split")&&(split!=null&&(split==1||split==2))){Long parsed=value.equalsIgnoreCase("none")?null:parseTimeTicks(value);if(split==1)s1=parsed;else s2=parsed;}
        else throw new IllegalArgumentException("TA編集fieldが不正です");
        return withTa(p,new TimeAttackResultSnapshot(null,time,reached,sequence,s1,s2));
    }

    private PlayerResultSnapshot editEndurance(PlayerResultSnapshot p,String field,String value){EnduranceResultSnapshot old=require(p.endurance(),"耐久記録");int progress=field.equals("progress")?nonnegativeInt(value):old.maxProgress();Long tick=field.equals("reached-tick")?nonnegativeLong(value):old.progressReachedTick();if(!field.equals("progress")&&!field.equals("reached-tick"))throw new IllegalArgumentException("fieldはprogress/reached-tickです");if(progress>0&&!maps.endurance().progresses().containsKey(progress))throw new IllegalArgumentException("存在しないProgressです: "+progress);if(progress>0&&tick==null)tick=0L;var c=maps.endurance();return withEndurance(p,new EnduranceResultSnapshot(null,progress,tick,c.zone2Progress()!=null&&progress>=c.zone2Progress(),c.zone3Progress()!=null&&progress>=c.zone3Progress(),c.goalProgress()!=null&&progress>=c.goalProgress()));}

    private <R> List<PlayerResultSnapshot> rank(List<PlayerResultSnapshot> players,Function<PlayerResultSnapshot,R> get,Comparator<R> order,RankWriter writer){List<PlayerResultSnapshot> eligible=players.stream().filter(p->get.apply(p)!=null).sorted(Comparator.comparing(get,order).thenComparing(PlayerResultSnapshot::uuid)).toList();Map<UUID,Integer> ranks=new HashMap<>();R previous=null;int rank=0;for(int i=0;i<eligible.size();i++){R current=get.apply(eligible.get(i));if(previous==null||order.compare(previous,current)!=0)rank=i+1;ranks.put(eligible.get(i).uuid(),rank);previous=current;}return players.stream().map(p->ranks.containsKey(p.uuid())?writer.write(p,ranks.get(p.uuid())):p).toList();}
    private static Comparator<HighResultSnapshot> highComparator(){return Comparator.comparingInt(HighResultSnapshot::points).reversed().thenComparingLong(r->r.points()==0?0:r.finalPointTick());}
    private static Comparator<TimeAttackResultSnapshot> taComparator(){return Comparator.comparing((TimeAttackResultSnapshot r)->r.pbTicks()==null).thenComparing(r->r.pbTicks(),Comparator.nullsLast(Long::compare)).thenComparing(r->r.pbRecordedTick(),Comparator.nullsLast(Long::compare)).thenComparing(r->r.pbRecordSequence(),Comparator.nullsLast(Long::compare));}
    private static Comparator<EnduranceResultSnapshot> enduranceComparator(){return Comparator.comparingInt(EnduranceResultSnapshot::maxProgress).reversed().thenComparingLong(r->r.maxProgress()==0?0:r.progressReachedTick());}
    private ResultsSnapshot flags(ResultsSnapshot c,Competition x,boolean confirmed,List<PlayerResultSnapshot> p){return new ResultsSnapshot(c.schemaVersion(),x==Competition.HIGH?confirmed:c.highConfirmed(),x==Competition.TA?confirmed:c.timeAttackConfirmed(),x==Competition.ENDURANCE?confirmed:c.enduranceConfirmed(),false,p);}
    private static PlayerResultSnapshot clearOverall(PlayerResultSnapshot p){return new PlayerResultSnapshot(p.uuid(),p.tournamentName(),p.participatedHigh(),p.participatedTa(),p.participatedEndurance(),p.overallExcluded(),p.disqualified(),p.high(),p.timeAttack(),p.endurance(),null);}
    private static PlayerResultSnapshot withHigh(PlayerResultSnapshot p,HighResultSnapshot r){return new PlayerResultSnapshot(p.uuid(),p.tournamentName(),p.participatedHigh(),p.participatedTa(),p.participatedEndurance(),p.overallExcluded(),p.disqualified(),r,p.timeAttack(),p.endurance(),null);}
    private static PlayerResultSnapshot withTa(PlayerResultSnapshot p,TimeAttackResultSnapshot r){return new PlayerResultSnapshot(p.uuid(),p.tournamentName(),p.participatedHigh(),p.participatedTa(),p.participatedEndurance(),p.overallExcluded(),p.disqualified(),p.high(),r,p.endurance(),null);}
    private static PlayerResultSnapshot withEndurance(PlayerResultSnapshot p,EnduranceResultSnapshot r){return new PlayerResultSnapshot(p.uuid(),p.tournamentName(),p.participatedHigh(),p.participatedTa(),p.participatedEndurance(),p.overallExcluded(),p.disqualified(),p.high(),p.timeAttack(),r,null);}
    private static TimeAttackResultSnapshot copyTa(TimeAttackResultSnapshot r,Integer rank){return new TimeAttackResultSnapshot(rank,r.pbTicks(),r.pbRecordedTick(),r.pbRecordSequence(),r.pbSplit1Ticks(),r.pbSplit2Ticks());}
    private static EnduranceResultSnapshot copyEndurance(EnduranceResultSnapshot r,Integer rank){return new EnduranceResultSnapshot(rank,r.maxProgress(),r.progressReachedTick(),r.zone2Reached(),r.zone3Reached(),r.goalReached());}
    private static PlayerResultSnapshot find(ResultsSnapshot r,String name){return r.players().stream().filter(p->p.tournamentName().equalsIgnoreCase(name)).findFirst().orElseThrow(()->new IllegalArgumentException("プレイヤーが見つかりません: "+name));}
    private static <T>T require(T value,String name){if(value==null)throw new IllegalArgumentException(name+"がありません");return value;}
    private static int nonnegativeInt(String v){try{int n=Integer.parseInt(v);if(n<0)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException("0以上の整数を指定してください");}}
    private static long nonnegativeLong(String v){try{long n=Long.parseLong(v);if(n<0)throw new NumberFormatException();return n;}catch(NumberFormatException e){throw new IllegalArgumentException("0以上の整数を指定してください");}}
    private static long nextSequence(ResultsSnapshot r){return r.players().stream().map(PlayerResultSnapshot::timeAttack).filter(Objects::nonNull).map(TimeAttackResultSnapshot::pbRecordSequence).filter(Objects::nonNull).mapToLong(Long::longValue).max().orElse(0)+1;}
    private static Object extractorValue(PlayerResultSnapshot p,Competition c){return switch(c){case HIGH->p.high();case TA->p.timeAttack();case ENDURANCE->p.endurance();};}
    private static Function<PlayerResultSnapshot,Object> extractor(Competition c){return p->extractorValue(p,c);}
    private static Integer rankOf(Object r){return switch(r){case HighResultSnapshot x->x.rank();case TimeAttackResultSnapshot x->x.rank();case EnduranceResultSnapshot x->x.rank();default->null;};}
    private static String describe(PlayerResultSnapshot p,Competition c,String field,Integer split){Object r=extractorValue(p,c);return String.valueOf(r)+(split==null?"":" split="+split);}
    @FunctionalInterface private interface RankWriter{PlayerResultSnapshot write(PlayerResultSnapshot player,Integer rank);}
}
