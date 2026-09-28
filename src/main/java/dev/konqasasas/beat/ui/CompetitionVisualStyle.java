package dev.konqasasas.beat.ui;

import dev.konqasasas.beat.configuration.ConfigurationFiles;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.ChatColor;

public final class CompetitionVisualStyle {
    private final ConfigurationFiles configuration;
    private static final Map<CompetitionNameColor, ChatColor> FALLBACKS = fallbacks();

    public CompetitionVisualStyle(ConfigurationFiles configuration) {
        this.configuration = configuration;
    }

    public String playerName(CompetitionNameColor color, String name) {
        return color(color) + name + ChatColor.RESET;
    }

    public String segment(CompetitionNameColor color, String text) {
        return color(color) + text + ChatColor.RESET;
    }

    public ChatColor color(CompetitionNameColor color) {
        String key = color == CompetitionNameColor.NEUTRAL
                ? "low-progress"
                : color.name().toLowerCase(java.util.Locale.ROOT);
        return configuration.styleColor("competition-name-colors." + key, FALLBACKS.get(color));
    }

    private static Map<CompetitionNameColor, ChatColor> fallbacks() {
        EnumMap<CompetitionNameColor, ChatColor> colors = new EnumMap<>(CompetitionNameColor.class);
        colors.put(CompetitionNameColor.NEUTRAL, ChatColor.RED);
        colors.put(CompetitionNameColor.CAUTION, ChatColor.YELLOW);
        colors.put(CompetitionNameColor.ACTIVE, ChatColor.AQUA);
        colors.put(CompetitionNameColor.ADVANCED, ChatColor.LIGHT_PURPLE);
        colors.put(CompetitionNameColor.LEADER, ChatColor.GOLD);
        colors.put(CompetitionNameColor.COMPLETE, ChatColor.GREEN);
        colors.put(CompetitionNameColor.DANGER, ChatColor.RED);
        colors.put(CompetitionNameColor.ELIMINATED, ChatColor.DARK_GRAY);
        return Map.copyOf(colors);
    }
}
