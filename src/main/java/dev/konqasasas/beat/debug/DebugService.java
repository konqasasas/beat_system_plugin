package dev.konqasasas.beat.debug;

import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.domain.endurance.EnduranceRules;
import dev.konqasasas.beat.domain.high.HighDifficultyRules;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import java.util.List;
import java.util.Map;

public final class DebugService {
    private final FormalResultGuard formalResults;
    private final DebugScenarioRunner scenarios = new DebugScenarioRunner();
    private final MapConfigurationService maps;
    private final int spotPoints;
    private final int goalPoints;
    private DebugSession session;
    private DebugCompetitionSimulator simulator;

    public DebugService(FormalResultGuard formalResults) { this(formalResults,null,100,500); }
    public DebugService(FormalResultGuard formalResults,MapConfigurationService maps,int spotPoints,int goalPoints) { this.formalResults=formalResults;this.maps=maps;this.spotPoints=spotPoints;this.goalPoints=goalPoints; }
    public boolean enabled(){return session!=null;}
    public DebugSession session(){if(session==null)throw new IllegalStateException("先に /beat debug enable を実行してください");return session;}

    public void enable() throws PersistenceException {
        if (session != null) throw new IllegalStateException("Debugは既に有効です");
        if (formalResults.exist()) throw new IllegalStateException("正式結果データが存在するためDebugを有効化できません");
        session = new DebugSession();
    }
    public void disable(){session=null;simulator=null;}
    public List<DebugScenarioRunner.ScenarioResult> runScenarios(String name){session();return scenarios.run(name);}

    public String trigger(String competition,String event,Integer value1,Integer value2){
        DebugSession s=session();DebugCompetitor target=s.triggerTarget();String name=target.name();DebugCompetitionSimulator simulation=simulator(s);
        switch(competition+":"+event){
            case "high:spot"->{int course=require(value1,"course"),spot=require(value2,"spot");simulation.high().reachSpot(target.uuid(),course,spot,s.tick());syncHigh(target,simulation);return name+" Course "+course+" Spot "+spot;}
            case "high:goal"->{int course=require(value1,"course");simulation.high().reachGoal(target.uuid(),course,s.tick());syncHigh(target,simulation);return name+" Course "+course+" Goal";}
            case "ta:start"->{boolean accepted=simulation.timeAttack().enterStart(target.uuid(),false,true,true,s.tick());if(!accepted)throw new IllegalStateException("TA startが受理されませんでした");return name+" TA Start";}
            case "ta:split"->{int split=require(value1,"split");var elapsed=simulation.timeAttack().reachSplit(target.uuid(),split,s.tick());if(elapsed.isEmpty())throw new IllegalStateException("TA splitが受理されませんでした");return name+" TA Split "+split+" "+elapsed.getAsLong()+" ticks";}
            case "ta:goal"->{var result=simulation.timeAttack().reachGoal(target.uuid(),s.tick());if(!result.valid())throw new IllegalStateException("先にTA startをtriggerしてください");var record=simulation.timeAttack().record(target.uuid());target.setTimeAttack(record.personalBestTicks().orElseThrow(),record.personalBestRecordedTick().orElseThrow(),record.personalBestRecordSequence().orElseThrow());return name+" TA Goal "+result.elapsedTicks()+" ticks";}
            case "endurance:progress"->{int progress=require(value1,"progress");simulation.endurance().reach(target.uuid(),progress,s.tick());syncEndurance(target,simulation);return name+" Progress "+progress;}
            case "endurance:zone"->{int zone=require(value1,"zone");if(zone!=2&&zone!=3)throw new IllegalArgumentException("zoneは2または3です");int progress=zone==2?enduranceRules().zone2Progress():enduranceRules().zone3Progress();simulation.endurance().reach(target.uuid(),progress,s.tick());syncEndurance(target,simulation);return name+" Zone "+zone;}
            case "endurance:goal"->{int progress=enduranceRules().goalProgress();simulation.endurance().reach(target.uuid(),progress,s.tick());syncEndurance(target,simulation);return name+" Endurance Goal";}
            default->throw new IllegalArgumentException("未対応のtriggerです: "+competition+" "+event);
        }
    }
    private DebugCompetitionSimulator simulator(DebugSession current){if(simulator==null||!simulator.matches(current.competitors()))simulator=new DebugCompetitionSimulator(current.competitors(),highRules(),enduranceRules());return simulator;}
    private HighDifficultyRules highRules(){if(maps==null||maps.high().courses().keySet().stream().noneMatch(x->x>=1&&x<=5))return new HighDifficultyRules(Map.of(1,3,2,3,3,3,4,3,5,3),spotPoints,goalPoints);Map<Integer,Integer> counts=new java.util.LinkedHashMap<>();for(int course=1;course<=5;course++){var configured=maps.high().courses().get(course);if(configured==null)return new HighDifficultyRules(Map.of(1,3,2,3,3,3,4,3,5,3),spotPoints,goalPoints);counts.put(course,configured.spots().size());}return new HighDifficultyRules(counts,spotPoints,goalPoints);}
    private EnduranceRules enduranceRules(){if(maps==null||maps.endurance().zone2Progress()==null||maps.endurance().zone3Progress()==null||maps.endurance().goalProgress()==null)return new EnduranceRules(40,70,100);return new EnduranceRules(maps.endurance().zone2Progress(),maps.endurance().zone3Progress(),maps.endurance().goalProgress());}
    private static void syncHigh(DebugCompetitor target,DebugCompetitionSimulator simulation){var record=simulation.high().record(target.uuid());target.setHigh(record.points(),record.finalPointTick().orElse(0));}
    private static void syncEndurance(DebugCompetitor target,DebugCompetitionSimulator simulation){var record=simulation.endurance().record(target.uuid());target.setEndurance(record.maxProgress(),record.maxProgressReachedTick().orElse(0));}
    private static int require(Integer value,String name){if(value==null)throw new IllegalArgumentException(name+"が必要です");return value;}
    @FunctionalInterface public interface FormalResultGuard { boolean exist() throws PersistenceException; }
}
