package dev.konqasasas.beat.spigot;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class CompetitionPlayerState {
    private CompetitionPlayerState(){}
    public static void normalize(Player player){
        player.setGameMode(GameMode.ADVENTURE);
        var maxHealth=player.getAttribute(Attribute.MAX_HEALTH);
        if(maxHealth!=null)player.setHealth(maxHealth.getValue());
        player.setFoodLevel(20);player.setSaturation(20);player.setFireTicks(0);player.setVelocity(new Vector());
        for(var effect:player.getActivePotionEffects())player.removePotionEffect(effect.getType());
        player.getInventory().clear();player.setCollidable(false);
    }
    public static void release(Player player){player.setCollidable(true);}
}
