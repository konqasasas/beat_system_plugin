package dev.konqasasas.beat.ui;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Session-scoped visibility preferences, independent from Bukkit and packets. */
public final class PlayerVisibilityPolicy {
    private final Set<UUID> hiddenViewers = new HashSet<>();
    public boolean toggle(UUID viewer){if(hiddenViewers.remove(viewer))return true;hiddenViewers.add(viewer);return false;}
    public boolean visible(UUID viewer){return !hiddenViewers.contains(viewer);}
    public boolean shouldHide(UUID viewer,UUID target,boolean targetParticipant,boolean targetAdmin){return !visible(viewer)&&!viewer.equals(target)&&targetParticipant&&!targetAdmin;}
    public void clear(){hiddenViewers.clear();}
}
