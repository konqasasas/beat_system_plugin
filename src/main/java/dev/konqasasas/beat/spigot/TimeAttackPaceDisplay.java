package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.domain.ta.TimeAttackPace;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/** Viewer-only TA pace chestplates; actual inventories are never modified. */
public final class TimeAttackPaceDisplay implements Listener {
    private final BeatPlugin plugin;
    private final AdminAuthorizer admins;
    private final Map<UUID, TimeAttackPace> paces = new HashMap<>();
    private Set<UUID> participants = Set.of();

    public TimeAttackPaceDisplay(BeatPlugin plugin, AdminAuthorizer admins) { this.plugin = plugin; this.admins = admins; }

    public void begin(Set<UUID> participantIds) {
        clearAll();
        participants = Set.copyOf(participantIds);
    }

    public void set(Player target, TimeAttackPace pace) {
        if (pace == TimeAttackPace.NONE) {
            clear(target);
            return;
        }
        if (pace == paces.put(target.getUniqueId(), pace)) return;
        sendToAudience(target, item(pace));
    }

    public void clear(Player target) {
        if (paces.remove(target.getUniqueId()) == null) return;
        sendToAudience(target, actualChestplate(target));
    }

    public void refreshViewer(Player viewer) {
        if (!audience(viewer)) return;
        for (var entry : paces.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target != null) viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, item(entry.getValue()));
        }
    }

    public void clearAll() {
        for (UUID id : java.util.List.copyOf(paces.keySet())) {
            Player target = Bukkit.getPlayer(id);
            if (target != null) sendToAudience(target, actualChestplate(target));
        }
        paces.clear();
        participants = Set.of();
    }

    public TimeAttackPace current(UUID playerId) {
        return paces.getOrDefault(playerId, TimeAttackPace.NONE);
    }

    public void preview(Player viewer, Player target, TimeAttackPace pace) {
        if (pace == TimeAttackPace.NONE) {
            paces.remove(target.getUniqueId());
            ItemStack actual = actualChestplate(target);
            viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, actual);
            target.sendEquipmentChange(target, EquipmentSlot.CHEST, actual);
            return;
        }
        paces.put(target.getUniqueId(), pace);
        ItemStack item = item(pace);
        viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, item);
        target.sendEquipmentChange(target, EquipmentSlot.CHEST, item);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshViewer(event.getPlayer()), 1L);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer()); }

    private void sendToAudience(Player target, ItemStack item) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (audience(viewer)) viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, item);
        }
    }

    private boolean audience(Player player) {
        return participants.contains(player.getUniqueId()) || admins.isAdmin(player.getUniqueId());
    }

    private static ItemStack actualChestplate(Player target) {
        ItemStack actual = target.getInventory().getChestplate();
        return actual == null ? new ItemStack(Material.AIR) : actual.clone();
    }

    private static ItemStack item(TimeAttackPace pace) {
        Material material = switch (pace) {
            case LEADER -> Material.NETHERITE_CHESTPLATE;
            case BORDER -> Material.DIAMOND_CHESTPLATE;
            case PERSONAL_BEST -> Material.IRON_CHESTPLATE;
            case NONE -> Material.AIR;
        };
        ItemStack item = new ItemStack(material);
        if (pace == TimeAttackPace.LEADER) {
            item.addEnchantment(Enchantment.UNBREAKING, 1);
            var meta = item.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
