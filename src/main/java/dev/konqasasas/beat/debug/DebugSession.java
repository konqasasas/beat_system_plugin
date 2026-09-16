package dev.konqasasas.beat.debug;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Volatile debug-only data. This type has no persistence dependency by design. */
public final class DebugSession {
    private static final long[] BOUNDARIES = {12_000, 18_000, 24_000, 30_000, 36_000};
    private final Map<String, DebugCompetitor> competitors = new LinkedHashMap<>();
    private long tick;
    private long sequence;

    public List<DebugCompetitor> competitors() { return List.copyOf(competitors.values()); }
    public long tick() { return tick; }

    public List<DebugCompetitor> addBots(int count) {
        if (count < 1 || count > 100) throw new IllegalArgumentException("bots数は1..100です");
        List<DebugCompetitor> added = new ArrayList<>();
        int next = 1;
        while (added.size() < count) {
            String name = "Debug%02d".formatted(next++);
            if (competitors.containsKey(key(name))) continue;
            UUID id = UUID.nameUUIDFromBytes(("beat-debug:" + name).getBytes(StandardCharsets.UTF_8));
            DebugCompetitor competitor = new DebugCompetitor(id, name);
            competitors.put(key(name), competitor);
            added.add(competitor);
        }
        return List.copyOf(added);
    }

    public void clear() { competitors.clear(); tick = 0; sequence = 0; }

    public DebugCompetitor require(String name) {
        DebugCompetitor competitor = competitors.get(key(name));
        if (competitor == null) throw new IllegalArgumentException("Debug参加者が見つかりません: " + name);
        return competitor;
    }

    public void setHigh(String name, int points, Long reachedTick) {
        if (points < 0) throw new IllegalArgumentException("pointsは0以上です");
        require(name).setHigh(points, reachedTick == null ? tick : reachedTick);
    }

    public void setTimeAttack(String name, long pbTicks, Long reachedTick) {
        if (pbTicks < 0) throw new IllegalArgumentException("TA timeは0以上です");
        require(name).setTimeAttack(pbTicks, reachedTick == null ? tick : reachedTick, ++sequence);
    }

    public void setEndurance(String name, int progress, Long reachedTick) {
        if (progress < 0) throw new IllegalArgumentException("progressは0以上です");
        require(name).setEndurance(progress, reachedTick == null ? tick : reachedTick);
    }

    public void fill(String competition, long seed) {
        ensureBots();
        Random random = new Random(seed);
        for (DebugCompetitor competitor : competitors.values()) {
            if (competition.equals("high") || competition.equals("all")) competitor.setHigh(random.nextInt(1201), random.nextLong(36_001));
            if (competition.equals("ta") || competition.equals("all")) {
                if (random.nextInt(10) < 9) competitor.setTimeAttack(450 + random.nextInt(451), random.nextLong(36_001), ++sequence);
                else competitor.clearTimeAttack();
            }
            if (competition.equals("endurance") || competition.equals("all")) competitor.setEndurance(random.nextInt(101), random.nextLong(36_001));
        }
    }

    public void generateCase(String competition, String caseName) {
        ensureMinimumBots(3);
        DebugCompetitor a = competitors().get(0), b = competitors().get(1), c = competitors().get(2);
        switch (competition + ":" + caseName) {
            case "high:tie" -> { a.setHigh(850, 1_000); b.setHigh(850, 1_000); }
            case "ta:tie" -> { a.setTimeAttack(570, 1_000, ++sequence); b.setTimeAttack(570, 1_000, ++sequence); }
            case "ta:no-record" -> c.clearTimeAttack();
            case "endurance:tie" -> { a.setEndurance(78, 1_000); b.setEndurance(78, 1_000); }
            case "overall:tie" -> { for (DebugCompetitor x : List.of(a,b)) { x.setHigh(900,1_000);x.setTimeAttack(570,1_000,++sequence);x.setEndurance(78,1_000); } }
            case "overall:disqualified" -> c.disqualified(true);
            case "overall:missing-participation" -> c.clearTimeAttack();
            default -> throw new IllegalArgumentException("未対応の特殊ケースです: " + competition + " " + caseName);
        }
    }

    public long setTime(long newTick) { if (newTick < 0) throw new IllegalArgumentException("tickは0以上です"); return tick = newTick; }
    public long advance(long ticks) { if (ticks < 0) throw new IllegalArgumentException("advanceは0以上です"); return tick = Math.addExact(tick, ticks); }
    public long next() {
        for (long boundary : BOUNDARIES) if (Math.max(0, boundary - 200) > tick) return tick = boundary - 200;
        throw new IllegalStateException("次の重要イベントはありません");
    }

    public DebugCompetitor triggerTarget() { ensureBots(); return competitors().getFirst(); }

    public List<Standing> highRanking() { return rank((a,b)->{int points=Integer.compare(b.highPoints(),a.highPoints());if(points!=0||a.highPoints()==0)return points;return Long.compare(a.highTick(),b.highTick());}); }
    public List<Standing> timeAttackRanking() { return rank(Comparator.comparing((DebugCompetitor c) -> c.taTicks() == null).thenComparing(c -> c.taTicks() == null ? Long.MAX_VALUE : c.taTicks()).thenComparingLong(DebugCompetitor::taReachedTick).thenComparingLong(DebugCompetitor::taSequence)); }
    public List<Standing> enduranceRanking() { return rank((a,b)->{int progress=Integer.compare(b.enduranceProgress(),a.enduranceProgress());if(progress!=0||a.enduranceProgress()==0)return progress;return Long.compare(a.enduranceTick(),b.enduranceTick());}); }

    private List<Standing> rank(Comparator<DebugCompetitor> order) {
        List<DebugCompetitor> sorted = new ArrayList<>(competitors.values());
        sorted.sort(order.thenComparing(DebugCompetitor::uuid));
        List<Standing> result = new ArrayList<>();
        DebugCompetitor previous = null; int rank = 0;
        for (int i=0;i<sorted.size();i++) { DebugCompetitor current=sorted.get(i); if(previous==null||order.compare(previous,current)!=0)rank=i+1;result.add(new Standing(rank,current));previous=current; }
        return List.copyOf(result);
    }

    private void ensureBots() { if (competitors.isEmpty()) addBots(1); }
    private void ensureMinimumBots(int count) { if (competitors.size() < count) addBots(count - competitors.size()); }
    private static String key(String name) { return name.toLowerCase(Locale.ROOT); }
    public record Standing(int rank, DebugCompetitor competitor) {}
}
