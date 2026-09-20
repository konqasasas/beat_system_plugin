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

public final class HighAdjustmentShield {
    private final BeatPlugin plugin;
    private final NamespacedKey marker;

    public HighAdjustmentShield(BeatPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "high-adjustment-shield");
    }

    public void give(Player player) {
        if (contains(player)) return;
        int slot = Math.max(0, Math.min(8,
                plugin.getConfig().getInt("high-difficulty.adjustment-shield.slot", 2)));
        player.getInventory().setItem(slot, create());
    }

    public void remove(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isShield(inventory.getItem(slot))) inventory.setItem(slot, null);
        }
    }

    boolean isShield(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                        .has(marker, PersistentDataType.BYTE);
    }

    private boolean contains(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isShield(item)) return true;
        }
        return false;
    }

    private ItemStack create() {
        String path = "high-difficulty.adjustment-shield";
        Material material = Material.matchMaterial(
                plugin.getConfig().getString(path + ".material", "SHIELD"));
        if (material == null || material.isAir()) material = Material.SHIELD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(plugin.getConfig().getString(
                path + ".name", "&f位置調整用の盾")));
        List<String> configuredLore = plugin.getConfig().getStringList(path + ".lore");
        List<String> lore = (configuredLore.isEmpty()
                ? List.of("&7構えて移動速度を落とし、細かく位置を調整できます。")
                : configuredLore).stream().map(HighAdjustmentShield::color).toList();
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
