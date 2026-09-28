package dev.konqasasas.beat.domain.notification;

public enum HighlightType {
    ALL_CLEAR(100, "all-clear"),
    LEADER_CHANGE(90, "leader-change"),
    LEADER_UPDATE(80, "leader-update"),
    GOAL(70, "goal"),
    COURSE_CLEAR(65, "milestone"),
    ZONE_3(65, "milestone"),
    ZONE_2(64, "milestone"),
    BORDER(60, "border"),
    PERSONAL_BEST(50, "personal-best"),
    RANK_CHANGE(10, "rank-change");

    private final int priority;
    private final String styleKey;
    HighlightType(int priority, String styleKey) { this.priority = priority; this.styleKey = styleKey; }
    public int priority() { return priority; }
    public String styleKey() { return styleKey; }
}
