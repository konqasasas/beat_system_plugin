package dev.konqasasas.beat.command;

import dev.konqasasas.beat.debug.DebugScenarioRunner;
import dev.konqasasas.beat.debug.DebugService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.spigot.HighPracticeController;
import dev.konqasasas.beat.spigot.LiveCompetitionClock;
import dev.konqasasas.beat.spigot.LiveCompetitionClockService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class DebugCommand {
    private static final List<String> ROOT=List.of("enable","disable","status","bots","high","high-practice","ta","endurance","fill","case","time","simulation","sound","trigger","scenario");
    private final DebugService debug;
    private final HighPracticeController highPractice;
    private final ConfigurationFiles messages;
    private final LiveCompetitionClockService liveClock;
    public DebugCommand(DebugService debug,HighPracticeController highPractice,ConfigurationFiles messages,LiveCompetitionClockService liveClock){this.debug=debug;this.highPractice=highPractice;this.messages=messages;this.liveClock=liveClock;}

    public boolean execute(CommandSender sender,String[] a){
        try{
            if(a.length==1&&eq(a[0],"enable")){debug.enable();sender.sendMessage(m("enabled","[BEAT] Debugを有効化しました。本番データには保存されません。"));return true;}
            if(a.length==1&&eq(a[0],"disable")){debug.disable();sender.sendMessage(m("disabled","[BEAT] Debugデータを破棄して無効化しました。"));return true;}
            if(a.length==1&&eq(a[0],"status")){sender.sendMessage(m("status","[BEAT] Debug: {status}",Map.of("status",debug.enabled()?"ENABLED / bots="+debug.session().competitors().size()+" / tick="+debug.session().tick():"DISABLED")));return true;}
            var s=debug.session();
            if(a.length==2&&eq(a[0],"high-practice")&&eq(a[1],"skip")){highPractice.skipPractice();sender.sendMessage(m("high-practice-skipped","[BEAT] 高難易度の練習をスキップし、準備フェーズへ進めました。"));return true;}
            if(a.length==3&&eq(a[0],"bots")&&eq(a[1],"add")){int count=integer(a[2],"count");s.addBots(count);sender.sendMessage(m("bots-added","[BEAT] Debug botsを追加しました: {count}",Map.of("count",count)));return true;}
            if(a.length==2&&eq(a[0],"bots")&&eq(a[1],"clear")){s.clear();sender.sendMessage(m("bots-cleared","[BEAT] Debug botsを全削除しました。"));return true;}
            if(a.length>=4&&eq(a[1],"set")&&eq(a[0],"high")){s.setHigh(a[2],integer(a[3],"points"),optionalReached(a,4));sender.sendMessage(m("record-set","[BEAT] Debug {competition}記録を投入しました。",Map.of("competition","High")));return true;}
            if(a.length>=4&&eq(a[1],"set")&&eq(a[0],"ta")){s.setTimeAttack(a[2],seconds(a[3]),optionalReached(a,4));sender.sendMessage(m("record-set","[BEAT] Debug {competition}記録を投入しました。",Map.of("competition","TA")));return true;}
            if(a.length>=4&&eq(a[1],"set")&&eq(a[0],"endurance")){s.setEndurance(a[2],integer(a[3],"progress"),optionalReached(a,4));sender.sendMessage(m("record-set","[BEAT] Debug {competition}記録を投入しました。",Map.of("competition","耐久")));return true;}
            if(a.length>=2&&eq(a[0],"fill")){String type=a[1].toLowerCase(Locale.ROOT);if(!List.of("high","ta","endurance","all").contains(type))throw new IllegalArgumentException("fill対象はhigh/ta/endurance/allです");long seed=a.length==4&&eq(a[2],"seed")?Long.parseLong(a[3]):0L;s.fill(type,seed);sender.sendMessage(m("filled","[BEAT] Debug {competition} をseed={seed}で生成しました。",Map.of("competition",type,"seed",seed)));return true;}
            if(a.length>=2&&eq(a[0],"case")){String competition,caseName;if(a.length==2&&List.of("disqualified","missing-participation").contains(a[1].toLowerCase(Locale.ROOT))){competition="overall";caseName=a[1];}else if(a.length==2&&a[1].contains("-")){int split=a[1].indexOf('-');competition=a[1].substring(0,split);caseName=a[1].substring(split+1);}else if(a.length==3){competition=a[1];caseName=a[2];}else throw new IllegalArgumentException("case指定が不正です");debug.session().generateCase(competition,caseName);sender.sendMessage(m("case-generated","[BEAT] 特殊ケースを生成しました: {competition} {case}",Map.of("competition",competition,"case",caseName)));return true;}
            if(a.length==2&&eq(a[0],"time")&&eq(a[1],"status")){sendClock(sender,liveClock.status());return true;}
            if(a.length==3&&eq(a[0],"time")&&eq(a[1],"set")){sendClock(sender,liveClock.set(clock(a[2])));return true;}
            if(a.length==3&&eq(a[0],"time")&&eq(a[1],"advance")){sendClock(sender,liveClock.advance(duration(a[2])));return true;}
            if(a.length==2&&eq(a[0],"time")&&eq(a[1],"next")){sendClock(sender,liveClock.next());return true;}
            if(a.length==3&&eq(a[0],"simulation")&&eq(a[1],"set")){long tick=s.setTime(clock(a[2]));sender.sendMessage(m("tick","[BEAT] Simulation tick = {tick}",Map.of("tick",tick)));return true;}
            if(a.length==3&&eq(a[0],"simulation")&&eq(a[1],"advance")){long tick=s.advance(duration(a[2]));sender.sendMessage(m("tick","[BEAT] Simulation tick = {tick}",Map.of("tick",tick)));return true;}
            if(a.length==2&&eq(a[0],"simulation")&&eq(a[1],"next")){sender.sendMessage(m("next","[BEAT] Simulationを次イベント10秒前へ移動: tick={tick}",Map.of("tick",s.next())));return true;}
            if(a.length==2&&eq(a[0],"sound")){playSound(sender,a[1]);return true;}
            if(a.length>=3&&eq(a[0],"trigger")){Integer v1=a.length>3?integer(a[3],"value"):null,v2=a.length>4?integer(a[4],"value"):null;sender.sendMessage(m("trigger","[BEAT] Trigger: {result}",Map.of("result",debug.trigger(a[1].toLowerCase(Locale.ROOT),a[2].toLowerCase(Locale.ROOT),v1,v2))));return true;}
            if(a.length==2&&eq(a[0],"scenario")){printScenarios(sender,debug.runScenarios(a[1]));return true;}
            help(sender);
        }catch(PersistenceException|IllegalArgumentException|IllegalStateException e){sender.sendMessage(m("error","[BEAT] DEBUG ERROR: {message}",Map.of("message",String.valueOf(e.getMessage()))));}
        return true;
    }

    public List<String> tab(String[] a){if(a.length==1)return matching(ROOT,a[0]);if(a.length==2&&eq(a[0],"high-practice"))return matching(List.of("skip"),a[1]);if(a.length==2&&eq(a[0],"bots"))return matching(List.of("add","clear"),a[1]);if(a.length==2&&eq(a[0],"fill"))return matching(List.of("high","ta","endurance","all"),a[1]);if(a.length==2&&eq(a[0],"case"))return matching(List.of("ta-tie","ta-no-record","high-tie","endurance-tie","overall-tie","disqualified","missing-participation"),a[1]);if(a.length==2&&(eq(a[0],"time")||eq(a[0],"simulation")))return matching(List.of("status","set","advance","next"),a[1]);if(a.length==2&&eq(a[0],"sound"))return matching(List.of("elimination","time-limit-end","ta-personal-best","ta-finished"),a[1]);if(a.length==2&&eq(a[0],"scenario"))return matching(List.of("all","high-boundaries","ta-elimination","endurance-zones","overall-ranking"),a[1]);if(a.length==2&&eq(a[0],"trigger"))return matching(List.of("high","ta","endurance"),a[1]);if(a.length==3&&List.of("high","ta","endurance").contains(a[0].toLowerCase(Locale.ROOT)))return matching(debug.enabled()?debug.session().competitors().stream().map(x->x.name()).toList():List.of(),a[2]);return List.of();}
    private void sendClock(CommandSender sender,LiveCompetitionClock.Snapshot snapshot){sender.sendMessage("[BEAT] "+snapshot.phase()+" "+clockText(snapshot.elapsedTick())+" / "+clockText(snapshot.totalTicks())+" (tick="+snapshot.elapsedTick()+")");}
    private void playSound(CommandSender sender,String key){if(!(sender instanceof Player player))throw new IllegalArgumentException("soundはゲーム内プレイヤー専用です");Sound fallback=switch(key.toLowerCase(Locale.ROOT)){case"elimination","time-limit-end"->Sound.BLOCK_NOTE_BLOCK_BASS;case"ta-personal-best"->Sound.ENTITY_PLAYER_LEVELUP;case"ta-finished"->Sound.BLOCK_NOTE_BLOCK_CHIME;default->throw new IllegalArgumentException("不明な音です: "+key);};String path="sounds."+key.toLowerCase(Locale.ROOT);player.playSound(player.getLocation(),messages.sound(path,fallback),messages.soundVolume(path,1F),messages.soundPitch(path,1F));sender.sendMessage("[BEAT] Sound test: "+key);}
    private static String clockText(long tick){long seconds=Math.max(0,(tick+19)/20);return "%02d:%02d".formatted(seconds/60,seconds%60);}
    private void printScenarios(CommandSender sender,List<DebugScenarioRunner.ScenarioResult> results){int passed=0,total=0;for(var result:results){sender.sendMessage(m("scenario-header","{primary}--- DEBUG SCENARIO: {name} ---{reset}",Map.of("name",result.name())));for(var check:result.checks()){sender.sendMessage(m(check.passed()?"scenario-check-pass":"scenario-check-fail",check.passed()?"{success}✓ {label}":"{error}✗ {label} expected={expected} actual={actual}",Map.of("label",check.label(),"expected",String.valueOf(check.expected()),"actual",String.valueOf(check.actual()))));}passed+=result.passed();total+=result.total();sender.sendMessage(m(result.passedAll()?"scenario-summary-pass":"scenario-summary-fail",result.passedAll()?"{success}PASS {passed} / {total}":"{error}FAIL {passed} / {total}",Map.of("passed",result.passed(),"total",result.total())));}if(results.size()>1)sender.sendMessage(m("all-summary","DEBUG ALL SCENARIOS: {passed} / {total} (FAIL {failed})",Map.of("passed",passed,"total",total,"failed",total-passed)));}
    private static Long optionalReached(String[] a,int index){if(a.length==index)return null;if(a.length==index+2&&eq(a[index],"reached"))return Long.parseLong(a[index+1]);throw new IllegalArgumentException("reached <tick> の形式で指定してください");}
    private static long seconds(String value){try{return new BigDecimal(value).multiply(BigDecimal.valueOf(20)).setScale(0,RoundingMode.UNNECESSARY).longValueExact();}catch(ArithmeticException|NumberFormatException e){throw new IllegalArgumentException("TA timeは0.05秒単位です: "+value);}}
    private static long clock(String value){String[] p=value.split(":",-1);if(p.length!=2)throw new IllegalArgumentException("時刻はmm:ss形式です");int m=integer(p[0],"minutes"),s=integer(p[1],"seconds");if(s>59)throw new IllegalArgumentException("secondsは0..59です");return (m*60L+s)*20;}
    private static long duration(String value){String v=value.toLowerCase(Locale.ROOT);if(v.endsWith("s"))return Long.parseLong(v.substring(0,v.length()-1))*20;if(v.endsWith("t"))return Long.parseLong(v.substring(0,v.length()-1));throw new IllegalArgumentException("時間は10sまたは200t形式です");}
    private static int integer(String value,String name){try{return Integer.parseInt(value);}catch(NumberFormatException e){throw new IllegalArgumentException(name+"は整数です: "+value);}}
    private static boolean eq(String a,String b){return a.equalsIgnoreCase(b);}private static List<String> matching(List<String> values,String input){String p=input.toLowerCase(Locale.ROOT);return values.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(p)).toList();}
    public void help(CommandSender s){messages.messages("commands.debug.help",List.of("{primary}--- BEAT Debug ---{reset}","/beat debug enable|disable|status","/beat debug high-practice skip","/beat debug time status|set <mm:ss>|advance <10s>|next - 実競技時計","/beat debug simulation set <mm:ss>|advance <10s>|next - 模擬時計","/beat debug sound elimination - 設定音を試聴","/beat debug bots add <count>|clear","/beat debug <high|ta|endurance> set <bot> <value> [reached <tick>]","/beat debug fill <high|ta|endurance|all> [seed <seed>]","/beat debug case <competition> <case>","/beat debug trigger <competition> <event> [values]","/beat debug scenario <name|all>")).forEach(s::sendMessage);}
    private String m(String key,String fallback){return messages.message("commands.debug."+key,fallback);}private String m(String key,String fallback,Map<String,?> values){return messages.message("commands.debug."+key,fallback,values);}
}
