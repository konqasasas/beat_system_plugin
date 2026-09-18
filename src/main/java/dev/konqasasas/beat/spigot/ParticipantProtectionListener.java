package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.roster.RosterService;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSignOpenEvent;

/** Prevents registered participants from modifying shared map data and containers. */
public final class ParticipantProtectionListener implements Listener {
    private static final Set<Material> PROTECTED_STORAGE_BLOCKS = EnumSet.of(
            Material.DECORATED_POT,
            Material.ACACIA_SHELF,
            Material.BAMBOO_SHELF,
            Material.BIRCH_SHELF,
            Material.CHERRY_SHELF,
            Material.CRIMSON_SHELF,
            Material.DARK_OAK_SHELF,
            Material.JUNGLE_SHELF,
            Material.MANGROVE_SHELF,
            Material.OAK_SHELF,
            Material.PALE_OAK_SHELF,
            Material.SPRUCE_SHELF,
            Material.WARPED_SHELF);

    private final RosterService rosters;
    private final AdminAuthorizer admins;

    public ParticipantProtectionListener(RosterService rosters, AdminAuthorizer admins) {
        this.rosters = Objects.requireNonNull(rosters, "rosters");
        this.admins = Objects.requireNonNull(admins, "admins");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!protectedParticipant(event.getPlayer().getUniqueId())
                || isPersonalInventory(event.getInventory().getType())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onStorageBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || !PROTECTED_STORAGE_BLOCKS.contains(event.getClickedBlock().getType())
                || !protectedParticipant(event.getPlayer().getUniqueId())) return;
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void onSignOpen(PlayerSignOpenEvent event) {
        if (protectedParticipant(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (protectedParticipant(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    private static boolean isPersonalInventory(InventoryType type) {
        return type == InventoryType.PLAYER || type == InventoryType.CRAFTING;
    }

    private boolean protectedParticipant(java.util.UUID playerId) {
        return rosters.current().participant(playerId).isPresent() && !admins.isAdmin(playerId);
    }
}
