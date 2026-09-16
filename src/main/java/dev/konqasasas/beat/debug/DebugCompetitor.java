package dev.konqasasas.beat.debug;

import java.util.UUID;

public final class DebugCompetitor {
    private final UUID uuid;
    private final String name;
    private int highPoints;
    private long highTick;
    private Long taTicks;
    private long taReachedTick;
    private long taSequence;
    private int enduranceProgress;
    private long enduranceTick;
    private boolean disqualified;

    DebugCompetitor(UUID uuid, String name) { this.uuid=uuid;this.name=name; }
    public UUID uuid(){return uuid;} public String name(){return name;}
    public int highPoints(){return highPoints;} public long highTick(){return highTick;}
    public Long taTicks(){return taTicks;} public long taReachedTick(){return taReachedTick;} public long taSequence(){return taSequence;}
    public int enduranceProgress(){return enduranceProgress;} public long enduranceTick(){return enduranceTick;}
    public boolean disqualified(){return disqualified;}
    void setHigh(int points,long tick){highPoints=points;highTick=tick;}
    void setTimeAttack(long ticks,long reachedTick,long sequence){taTicks=ticks;taReachedTick=reachedTick;taSequence=sequence;}
    void clearTimeAttack(){taTicks=null;taReachedTick=0;taSequence=0;}
    void setEndurance(int progress,long tick){enduranceProgress=progress;enduranceTick=tick;}
    void disqualified(boolean value){disqualified=value;}
}
