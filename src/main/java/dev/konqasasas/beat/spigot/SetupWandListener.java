package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import java.util.Map;
import dev.konqasasas.beat.setup.SelectionException;
import dev.konqasasas.beat.setup.SelectionPoint;
import dev.konqasasas.beat.setup.SelectionService;
import dev.konqasasas.beat.setup.SetupWand;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SetupWandListener implements Listener {
    private final SetupWand wand;
    private final SelectionService selections;
    private final AdminAuthorizer admins;
    private final ConfigurationFiles messages;

    public SetupWandListener(SetupWand wand, SelectionService selections, AdminAuthorizer admins,
            ConfigurationFiles messages) {
        this.wand = wand;
        this.selections = selections;
        this.admins = admins;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!wand.isWand(event.getItem())) return;
        event.setCancelled(true);
        if (!admins.isAdmin(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(messages.message("setup-wand.no-permission",
                    "[BEAT] このツールを使用する権限がありません。"));
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) return;
        SelectionPoint point = new SelectionPoint(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selections.setPos1(event.getPlayer().getUniqueId(), point);
            event.getPlayer().sendMessage(format("Pos1", point));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            try {
                selections.setPos2(event.getPlayer().getUniqueId(), point);
                event.getPlayer().sendMessage(format("Pos2", point));
            } catch (SelectionException exception) {
                event.getPlayer().sendMessage(messages.message("setup-wand.error", "[BEAT] ERROR: {message}",
                        Map.of("message", exception.getMessage())));
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (wand.isWand(event.getPlayer().getInventory().getItemInMainHand())) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (wand.isWand(event.getPlayer().getInventory().getItem(event.getHand()))) event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player
                && wand.isWand(player.getInventory().getItemInMainHand())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.clear(event.getPlayer().getUniqueId());
    }

    private String format(String name, SelectionPoint point) {
        return messages.message("setup-wand.selected", "[BEAT] {position}: {world} ({x}, {y}, {z})",
                Map.of("position", name, "world", point.world(), "x", point.x(), "y", point.y(), "z", point.z()));
    }
}
