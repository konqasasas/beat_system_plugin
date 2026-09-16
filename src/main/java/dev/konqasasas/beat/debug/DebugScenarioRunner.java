package dev.konqasasas.beat.debug;

import dev.konqasasas.beat.domain.Competitor;
import dev.konqasasas.beat.domain.endurance.EnduranceFallPolicy;
import dev.konqasasas.beat.domain.endurance.EnduranceRules;
import dev.konqasasas.beat.domain.endurance.EnduranceSession;
import dev.konqasasas.beat.domain.high.HighCompetitionSession;
import dev.konqasasas.beat.domain.high.HighDifficultyRecord;
import dev.konqasasas.beat.domain.high.HighDifficultyRules;
import dev.konqasasas.beat.domain.ranking.CompetitionRankings;
import dev.konqasasas.beat.domain.ranking.OverallRanking;
import dev.konqasasas.beat.domain.ranking.RankingEntry;
import dev.konqasasas.beat.domain.ta.TimeAttackSession;
import dev.konqasasas.beat.domain.time.CompetitionSchedule;
import dev.konqasasas.beat.persistence.snapshot.EnduranceResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.HighResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import dev.konqasasas.beat.persistence.snapshot.TimeAttackResultSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DebugScenarioRunner {
    private static final List<String> NAMES = List.of(
            "high-boundaries", "ta-elimination", "endurance-zones", "overall-ranking");

    public List<ScenarioResult> run(String name) {
        if (!name.equalsIgnoreCase("all")) return List.of(runOne(name));
        List<ScenarioResult> results = new ArrayList<>();
        for (String scenario : NAMES) {
            try { results.add(runOne(scenario)); }
            catch (RuntimeException exception) {
                results.add(new ScenarioResult(scenario, List.of(new Check(
                        "シナリオ実行", false, "正常終了", exception.getClass().getSimpleName()+": "+exception.getMessage()))));
            }
        }
        return List.copyOf(results);
    }

    public List<String> names() { return NAMES; }

    private ScenarioResult runOne(String name) {
        return switch (name.toLowerCase()) {
            case "high-boundaries" -> high();
            case "ta-elimination" -> timeAttack();
            case "endurance-zones" -> endurance();
            case "overall-ranking" -> overall();
            default -> throw new IllegalArgumentException("シナリオが見つかりません: " + name);
        };
    }

    private ScenarioResult high() {
        List<Check> checks = new ArrayList<>();
        HighDifficultyRules rules = highRules();
        Map<UUID,String> ids=names(6);
        HighCompetitionSession session=new HighCompetitionSession(ids,rules);
        ids.keySet().forEach(session::activate);
        UUID a=id("A"),b=id("B"),c=id("C"),d=id("D"),e=id("E"),f=id("F");

        session.reachSpot(a,1,3,200);
        check(checks,"Spot 補完",session.record(a).points()==300,300,session.record(a).points());
        session.reachSpot(b,1,3,400);
        check(checks,"同ポイント異 tick",session.rank(a)<session.rank(b),"Aが上位",session.rank(a)+"/"+session.rank(b));
        session.reachSpot(e,1,2,500);session.reachSpot(f,1,2,500);
        check(checks,"同ポイント同 tick",session.rank(e)==session.rank(f),"同率",session.rank(e)+"/"+session.rank(f));
        check(checks,"0pt 同率",session.rank(c)==session.rank(d),"同率",session.rank(c)+"/"+session.rank(d));
        session.reachGoal(a,2,600);
        check(checks,"Goal 全 Spot 補完",session.record(a).points()==1100,1100,session.record(a).points());

        CompetitionSchedule schedule=new CompetitionSchedule(List.of(12_000L,18_000L,24_000L,30_000L),36_000);
        check(checks,"10:00 Course2",schedule.isEliminationTick(12_000),true,false);
        check(checks,"15:00 Course3",schedule.isEliminationTick(18_000),true,false);
        check(checks,"20:00 Course4",schedule.isEliminationTick(24_000),true,false);
        check(checks,"25:00 Course5",schedule.isEliminationTick(30_000),true,false);
        check(checks,"30:00 Goal",schedule.acceptsEvents(36_000)&&schedule.isFinishTick(36_000),true,false);

        session.reachGoal(a,1,700);session.reachGoal(a,3,800);session.reachGoal(a,4,900);session.reachGoal(a,5,1000);
        check(checks,"Course5 all clear",session.record(a).allCoursesCleared(),true,session.record(a).allCoursesCleared());
        session.eliminateBelowCourse(2);int frozen=session.record(b).points();
        try{session.reachSpot(b,2,1,1100);}catch(IllegalStateException ignored){}
        check(checks,"脱落後記録固定",session.record(b).points()==frozen,frozen,session.record(b).points());
        return new ScenarioResult("high-boundaries",checks);
    }

    private ScenarioResult timeAttack() {
        List<Check> checks=new ArrayList<>();
        TimeAttackSession pb=session(3);UUID a=id("A"),b=id("B"),c=id("C");
        run(pb,a,0,600);run(pb,b,0,500);int before=pb.rank(a);
        var updateNoRank=run(pb,a,700,1250);
        check(checks,"PB更新",updateNoRank.personalBest()&&pb.record(a).personalBestTicks().orElseThrow()==550,550,pb.record(a).personalBestTicks().orElse(-1));
        check(checks,"PB更新順位不変",before==pb.rank(a),before,pb.rank(a));
        var updateRank=run(pb,a,1300,1750);
        check(checks,"PB更新順位変動",updateRank.rankChanged()&&pb.rank(a)==1,1,pb.rank(a));
        var slower=run(pb,a,1800,2300);
        check(checks,"PB非更新",!slower.personalBest()&&pb.record(a).personalBestTicks().orElseThrow()==450,450,pb.record(a).personalBestTicks().orElse(-1));

        TimeAttackSession events=session(1);
        boolean entered=events.enterStart(a,false,true,true,100);
        check(checks,"Start再進入",entered&&!events.enterStart(a,true,true,true,110),"2回目false",entered);
        check(checks,"Split飛ばし",events.reachSplit(a,2,200).orElseThrow()==100,100,events.reachSplit(a,1,200).orElse(-1));
        events.markRestarted(a);
        check(checks,"restart item",events.running(a)&&events.displayedElapsed(a,300)==0,"running/0",events.running(a)+"/"+events.displayedElapsed(a,300));

        TimeAttackSession tickTie=session(3);
        run(tickTie,a,0,500);run(tickTie,b,100,600);
        check(checks,"同タイム異記録tick",tickTie.rank(a)<tickTie.rank(b),"Aが上位",tickTie.rank(a)+"/"+tickTie.rank(b));
        TimeAttackSession sequenceTie=session(3);
        run(sequenceTie,a,0,500);run(sequenceTie,b,0,500);
        check(checks,"同タイム同tick recordSequence",sequenceTie.rank(a)<sequenceTie.rank(b),"先着A",sequenceTie.rank(a)+"/"+sequenceTie.rank(b));
        check(checks,"記録なし",sequenceTie.rank(c)>sequenceTie.rank(b),"Cが最下位",sequenceTie.rank(c));

        CompetitionSchedule schedule=new CompetitionSchedule(List.of(24_000L,30_000L),36_000);
        check(checks,"20:00 Goal",schedule.acceptsEvents(24_000)&&schedule.isEliminationTick(24_000),true,false);
        check(checks,"25:00 Goal",schedule.acceptsEvents(30_000)&&schedule.isEliminationTick(30_000),true,false);
        check(checks,"30:00 Goal",schedule.acceptsEvents(36_000)&&schedule.isFinishTick(36_000),true,false);

        TimeAttackSession border=session(2);run(border,a,23_500,24_000);var removed=border.eliminateToTop(1);
        check(checks,"脱落境界",border.active(a)&&removed.contains(b),"Goal反映後A生存",removed);

        TimeAttackSession dq=session(3);run(dq,a,0,400);run(dq,b,0,500);run(dq,c,0,600);dq.competitor(a).setDisqualified(true);dq.eliminateToTop(1);
        check(checks,"失格者が生存人数を消費しない",!dq.active(a)&&dq.active(b)&&!dq.active(c),"Bのみ生存",List.of(dq.active(a),dq.active(b),dq.active(c)));

        TimeAttackSession disconnected=session(1);disconnected.enterStart(a,false,true,true,100);
        check(checks,"切断中タイマー",disconnected.displayedElapsed(a,300)==200,200,disconnected.displayedElapsed(a,300));
        return new ScenarioResult("ta-elimination",checks);
    }

    private ScenarioResult endurance() {
        List<Check> checks=new ArrayList<>();Map<UUID,String> ids=names(4);
        EnduranceSession session=new EnduranceSession(ids,new EnduranceRules(40,70,100));ids.keySet().forEach(session::activate);
        UUID a=id("A"),b=id("B"),c=id("C"),d=id("D");
        session.reach(a,78,500);
        check(checks,"Progress飛ばし",session.record(a).maxProgress()==78,78,session.record(a).maxProgress());
        session.reach(b,78,500);
        check(checks,"Progress同率",session.rank(a)==session.rank(b),"同率",session.rank(a)+"/"+session.rank(b));
        check(checks,"Zone到達",session.record(a).zone2Reached()&&session.record(a).zone3Reached(),true,session.zone(a));
        session.reach(c,40,24_000);var zone2Eliminated=session.eliminateWithoutZone(2);
        check(checks,"Zone + Progress",zone2Eliminated.stream().noneMatch(x->x.id().equals(c)),"C生存",zone2Eliminated);
        check(checks,"Progress + fall",EnduranceFallPolicy.shouldRestart(session.record(c),-10,0)&&session.record(c).maxProgress()==40,"restart/40",session.record(c).maxProgress());
        check(checks,"Zone + fall",EnduranceFallPolicy.shouldRestart(session.record(a),-10,0)&&session.zone(a)==3,"restart/Zone3",session.zone(a));
        session.reach(a,100,30_000);
        check(checks,"Goal + fall",!EnduranceFallPolicy.shouldRestart(session.record(a),-10,0)&&session.record(a).goalReached(),"no restart",session.record(a).goalReached());
        CompetitionSchedule schedule=new CompetitionSchedule(List.of(24_000L,30_000L),36_000);
        check(checks,"20:00 Zone2",schedule.acceptsEvents(24_000)&&schedule.isEliminationTick(24_000),true,false);
        check(checks,"25:00 Zone3",schedule.acceptsEvents(30_000)&&schedule.isEliminationTick(30_000),true,false);
        check(checks,"30:00 Goal",schedule.acceptsEvents(36_000)&&schedule.isFinishTick(36_000),true,false);
        check(checks,"Progress000",session.record(d).maxProgress()==0,0,session.record(d).maxProgress());
        return new ScenarioResult("endurance-zones",checks);
    }

    private ScenarioResult overall() {
        List<Check> checks=new ArrayList<>();
        var normal=OverallRanking.calculate(List.of(player("A",1,2,3,false,false,true),player("B",2,3,4,false,false,true)));
        check(checks,"通常順位積",normal.getFirst().scoreProduct()==6,6,normal.getFirst().scoreProduct());
        var productTie=OverallRanking.calculate(List.of(player("A",1,3,4,false,false,true),player("B",2,2,3,false,false,true)));
        check(checks,"持ち点同値",productTie.stream().allMatch(x->x.scoreProduct()==12),"12/12",productTie.stream().map(OverallRanking.Standing::scoreProduct).toList());
        check(checks,"最高順位比較",productTie.getFirst().player().tournamentName().equals("A"),"A",productTie.getFirst().player().tournamentName());
        var secondTie=OverallRanking.calculate(List.of(player("C",1,4,6,false,false,true),player("D",1,3,8,false,false,true)));
        check(checks,"2番目比較",secondTie.getFirst().player().tournamentName().equals("D"),"D",secondTie.getFirst().player().tournamentName());
        var trueTie=OverallRanking.calculate(List.of(player("T1",2,2,2,false,false,true),player("T2",2,2,2,false,false,true)));
        check(checks,"完全同率",trueTie.get(0).rank()==trueTie.get(1).rank(),"同率",trueTie.stream().map(OverallRanking.Standing::rank).toList());
        check(checks,"1競技不参加",OverallRanking.calculate(List.of(player("Missing",1,1,1,false,false,false))).isEmpty(),true,false);
        var noRecord=player("NoRecord",1,9,1,false,false,true);
        check(checks,"TA記録なしでも参加済み",noRecord.timeAttack().pbTicks()==null&&!OverallRanking.calculate(List.of(noRecord)).isEmpty(),true,false);
        check(checks,"overallExcluded",OverallRanking.calculate(List.of(player("Excluded",1,1,1,true,false,true))).isEmpty(),true,false);
        check(checks,"disqualified",OverallRanking.calculate(List.of(player("DQ",1,1,1,false,true,true))).isEmpty(),true,false);

        HighDifficultyRules rules=highRules();HighDifficultyRecord first=new HighDifficultyRecord(rules);first.reachGoal(1,1);
        HighDifficultyRecord dqRecord=new HighDifficultyRecord(rules);dqRecord.reachSpot(1,2,2);
        HighDifficultyRecord third=new HighDifficultyRecord(rules);third.reachSpot(1,1,3);
        RankingEntry<HighDifficultyRecord> e1=entry("First",first),e2=entry("DQSeat",dqRecord),e3=entry("Third",third);e2.competitor().setDisqualified(true);
        var seats=CompetitionRankings.highDifficulty(List.of(e1,e2,e3));
        check(checks,"失格者による順位非繰上げ",seats.get(2).rank()==3&&seats.get(1).disqualified(),"Third=#3",seats.stream().map(x->x.rank()+":"+x.competitor().tournamentName()).toList());
        return new ScenarioResult("overall-ranking",checks);
    }

    private static TimeAttackSession session(int count){var s=new TimeAttackSession(names(count));names(count).keySet().forEach(s::activate);return s;}
    private static TimeAttackSession.GoalResult run(TimeAttackSession session,UUID id,long start,long goal){session.enterStart(id,false,true,true,start);return session.reachGoal(id,goal);}
    private static HighDifficultyRules highRules(){return new HighDifficultyRules(Map.of(1,3,2,3,3,3,4,3,5,3),100,500);}
    private static RankingEntry<HighDifficultyRecord> entry(String name,HighDifficultyRecord record){return new RankingEntry<>(new Competitor(id(name),name),record);}
    private static void check(List<Check> checks,String label,boolean passed,Object expected,Object actual){checks.add(new Check(label,passed,String.valueOf(expected),String.valueOf(actual)));}
    private static Map<UUID,String> names(int count){Map<UUID,String> result=new LinkedHashMap<>();for(char x='A';x<'A'+count;x++){String name=String.valueOf(x);result.put(id(name),name);}return result;}
    private static UUID id(String name){return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));}
    private static PlayerResultSnapshot player(String name,int high,int ta,int endurance,boolean excluded,boolean dq,boolean participated){return new PlayerResultSnapshot(id(name),name,participated,participated,participated,excluded,dq,new HighResultSnapshot(high,0,null),new TimeAttackResultSnapshot(ta,null,null,null,null,null),new EnduranceResultSnapshot(endurance,0,null,false,false,false),null);}

    public record Check(String label,boolean passed,String expected,String actual){}
    public record ScenarioResult(String name,List<Check> checks){public long passed(){return checks.stream().filter(Check::passed).count();}public int total(){return checks.size();}public boolean passedAll(){return passed()==total();}}
}
