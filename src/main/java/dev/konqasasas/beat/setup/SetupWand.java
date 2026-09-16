package dev.konqasasas.beat.setup;

import dev.konqasasas.beat.BeatPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class SetupWand {
    private final BeatPlugin plugin;
    private final NamespacedKey marker;

    public SetupWand(BeatPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "setup-wand");
    }

    public boolean give(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isWand(item)) return false;
        }
        player.getInventory().addItem(create()).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        return true;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(marker, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private ItemStack create() {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.matchMaterial(config.getString("setup.wand.material", "BLAZE_ROD"));
        if (material == null || material.isAir()) material = Material.BLAZE_ROD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(config.getString("setup.wand.name", "&bBEAT 範囲選択ツール")));
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("setup.wand.lore")) lore.add(color(line));
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
