package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.domain.state.TournamentState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public final class CompetitionSafetyListener implements Listener {
    private final EventStateService eventState;

    public CompetitionSafetyListener(EventStateService eventState) { this.eventState = eventState; }

    @EventHandler public void damage(EntityDamageEvent event){if(event.getEntity() instanceof Player)event.setCancelled(true);}
    @EventHandler public void hunger(FoodLevelChangeEvent event){if(event.getEntity() instanceof Player player){event.setCancelled(true);player.setFoodLevel(20);player.setSaturation(20);}}
    @EventHandler public void drop(PlayerDropItemEvent event){if(active(eventState.current().tournamentState()))event.setCancelled(true);}
    static boolean active(TournamentState state){return switch(state){case HIGH_PRACTICE_COUNTDOWN,HIGH_PRACTICE,HIGH_PREPARE,HIGH_RUNNING,TA_COUNTDOWN,TA_RUNNING,ENDURANCE_COUNTDOWN,ENDURANCE_RUNNING->true;default->false;};}
}
