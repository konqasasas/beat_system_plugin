package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class HighPracticeItems {
    public enum Kind { FLIGHT, CHECKPOINT }

    private final BeatPlugin plugin;
    private final NamespacedKey kindKey;

    public HighPracticeItems(BeatPlugin plugin) {
        this.plugin = plugin;
        this.kindKey = new NamespacedKey(plugin, "high-practice-item");
    }

    public void give(Player player) {
        if (!contains(player, Kind.CHECKPOINT)) place(player, Kind.CHECKPOINT);
        if (!contains(player, Kind.FLIGHT)) place(player, Kind.FLIGHT);
    }

    public void remove(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (kind(inventory.getItem(slot)) != null) inventory.setItem(slot, null);
        }
    }

    public Kind kind(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String value = item.getItemMeta().getPersistentDataContainer()
                .get(kindKey, PersistentDataType.STRING);
        if (value == null) return null;
        try {
            return Kind.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean contains(Player player, Kind expected) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (kind(item) == expected) return true;
        }
        return false;
    }

    private void place(Player player, Kind kind) {
        String path = kind == Kind.FLIGHT
                ? "high-difficulty.practice-items.flight.slot"
                : "high-difficulty.practice-items.checkpoint.slot";
        int fallback = kind == Kind.CHECKPOINT ? 0 : 1;
        int slot = Math.max(0, Math.min(8, plugin.getConfig().getInt(path, fallback)));
        ItemStack item = create(kind);
        ItemStack existing = player.getInventory().getItem(slot);
        if (existing == null || existing.getType().isAir()) player.getInventory().setItem(slot, item);
        else player.getInventory().addItem(item);
    }

    private ItemStack create(Kind kind) {
        String path = kind == Kind.FLIGHT ? "high-difficulty.practice-items.flight" : "high-difficulty.practice-items.checkpoint";
        Material fallback = kind == Kind.FLIGHT ? Material.FEATHER : Material.RECOVERY_COMPASS;
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", fallback.name()));
        if (material == null || material.isAir()) material = fallback;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(plugin.getConfig().getString(path + ".name",
                kind == Kind.FLIGHT ? "&b練習用飛行切替" : "&a練習用個人CP")));
        List<String> lore = plugin.getConfig().getStringList(path + ".lore").stream()
                .map(HighPracticeItems::color).toList();
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(kindKey, PersistentDataType.STRING, kind.name());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
