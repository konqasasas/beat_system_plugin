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

public final class HighRunningItem {
    private final BeatPlugin plugin;
    private final NamespacedKey marker;

    public HighRunningItem(BeatPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "high-return-item");
    }

    public boolean isReturnItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(marker, PersistentDataType.BYTE);
    }

    public void give(Player player) {
        for (ItemStack item : player.getInventory().getContents()) if (isReturnItem(item)) return;
        player.getInventory().addItem(create());
    }

    public void remove(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (isReturnItem(player.getInventory().getItem(slot))) player.getInventory().setItem(slot, null);
        }
    }

    private ItemStack create() {
        String path = "high-difficulty.running-item";
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "RECOVERY_COMPASS"));
        if (material == null || material.isAir()) material = Material.RECOVERY_COMPASS;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(plugin.getConfig().getString(path + ".name", "&e現在CourseのStartへ戻る")));
        meta.setLore(plugin.getConfig().getStringList(path + ".lore").stream().map(HighRunningItem::color).toList());
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
