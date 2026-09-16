package dev.konqasasas.beat.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns reloadable config, messages and visual styles for the plugin. */
public final class ConfigurationFiles {
    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private YamlConfiguration styles;

    private ConfigurationFiles(JavaPlugin plugin, YamlConfiguration messages, YamlConfiguration styles) {
        this.plugin = plugin;
        this.messages = messages;
        this.styles = styles;
    }

    public static ConfigurationFiles load(JavaPlugin plugin) throws ConfigurationLoadException {
        validate(new File(plugin.getDataFolder(), "config.yml"));
        return new ConfigurationFiles(
                plugin,
                read(new File(plugin.getDataFolder(), "messages.yml")),
                read(new File(plugin.getDataFolder(), "styles.yml")));
    }

    /** Validates every YAML file before replacing any live configuration reference. */
    public synchronized void reload() throws ConfigurationLoadException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration nextMessages = read(new File(plugin.getDataFolder(), "messages.yml"));
        YamlConfiguration nextStyles = read(new File(plugin.getDataFolder(), "styles.yml"));
        validate(configFile);
        plugin.reloadConfig();
        messages = nextMessages;
        styles = nextStyles;
    }

    public String message(String path, String fallback) {
        return color(applyStyleColors(messages.getString(path, fallback)));
    }

    public String message(String path, String fallback, Map<String, ?> placeholders) {
        return MessageTemplates.render(message(path, fallback), placeholders);
    }

    public List<String> messages(String path, List<String> fallback) {
        List<String> configured = messages.getStringList(path);
        List<String> selected = configured.isEmpty() ? fallback : configured;
        return selected.stream().map(this::applyStyleColors).map(ConfigurationFiles::color).toList();
    }

    public List<String> messages(String path, List<String> fallback, Map<String, ?> placeholders) {
        return messages(path, fallback).stream()
                .map(message -> MessageTemplates.render(message, placeholders))
                .toList();
    }

    public int configInt(String path, int fallback, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, plugin.getConfig().getInt(path, fallback)));
    }

    public Material material(String path, Material fallback) {
        Material configured = Material.matchMaterial(plugin.getConfig().getString(path, fallback.name()));
        return configured == null || configured.isAir() ? fallback : configured;
    }

    public String actionBarNotification(String message) {
        return message(
                "ui.actionbar.notification",
                "{warning}!! {reset}{message} {warning}!!",
                Map.of("message", ActionBarNotificationFormatter.colorize(message)));
    }

    public long configLong(String path, long fallback, long minimum, long maximum) {
        return Math.max(minimum, Math.min(maximum, plugin.getConfig().getLong(path, fallback)));
    }

    public double configDouble(String path, double fallback, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, plugin.getConfig().getDouble(path, fallback)));
    }

    public Sound sound(String path, Sound fallback) {
        String raw = styles.getString(path + ".name", fallback.getKeyOrThrow().toString());
        Sound configured = Registry.SOUNDS.match(raw);
        if (configured == null) {
            String normalized = normalizeSoundName(raw);
            for (Sound candidate : Registry.SOUNDS) {
                if (normalizeSoundName(candidate.getKeyOrThrow().getKey()).equals(normalized)) {
                    configured = candidate;
                    break;
                }
            }
        }
        return configured == null ? fallback : configured;
    }

    static String normalizeSoundName(String value) {
        int namespace = value.indexOf(':');
        String key = namespace >= 0 ? value.substring(namespace + 1) : value;
        return key.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public float soundVolume(String path, float fallback) {
        return (float) styles.getDouble(path + ".volume", fallback);
    }

    public float soundPitch(String path, float fallback) {
        return (float) styles.getDouble(path + ".pitch", fallback);
    }

    public int styleInt(String path, int fallback, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, styles.getInt(path, fallback)));
    }

    public double styleDouble(String path, double fallback, double minimum) {
        return Math.max(minimum, styles.getDouble(path, fallback));
    }

    public Particle particle(String path, Particle fallback) {
        try {
            return Particle.valueOf(styles.getString(path, fallback.name()).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public Particle.DustOptions dustOptions(String path, Color fallbackColor, float fallbackSize) {
        List<Integer> rgb = styles.getIntegerList(path + ".color");
        Color color = fallbackColor;
        if (rgb.size() == 3 && rgb.stream().allMatch(value -> value >= 0 && value <= 255)) {
            color = Color.fromRGB(rgb.get(0), rgb.get(1), rgb.get(2));
        }
        float size = (float) Math.max(0.01D, Math.min(4D, styles.getDouble(path + ".size", fallbackSize)));
        return new Particle.DustOptions(color, size);
    }

    public BarColor barColor(String path, BarColor fallback) {
        try {
            return BarColor.valueOf(styles.getString(path + ".color", fallback.name())
                    .toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    public BarStyle barStyle(String path, BarStyle fallback) {
        try {
            return BarStyle.valueOf(styles.getString(path + ".style", fallback.name())
                    .toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    @SuppressWarnings("deprecation")
    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private String applyStyleColors(String value) {
        String styled = value.replace("[BEAT]", "{beat}");
        String beat = styleColor("colors.brand", ChatColor.DARK_AQUA) + "[BEAT]" + ChatColor.RESET;
        return styled
                .replace("{beat}", beat)
                .replace("{primary}", styleColor("colors.primary", ChatColor.AQUA))
                .replace("{success}", styleColor("colors.success", ChatColor.GREEN))
                .replace("{warning}", styleColor("colors.warning", ChatColor.YELLOW))
                .replace("{error}", styleColor("colors.error", ChatColor.RED))
                .replace("{reset}", ChatColor.RESET.toString());
    }

    private String styleColor(String path, ChatColor fallback) {
        try {
            ChatColor selected = ChatColor.valueOf(
                    styles.getString(path, fallback.name()).toUpperCase(java.util.Locale.ROOT));
            return selected.isColor() ? selected.toString() : fallback.toString();
        } catch (IllegalArgumentException exception) {
            return fallback.toString();
        }
    }

    private static void validate(File file) throws ConfigurationLoadException {
        read(file);
    }

    private static YamlConfiguration read(File file) throws ConfigurationLoadException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
            return yaml;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new ConfigurationLoadException("設定ファイルを読み込めません: " + file.getName(), exception);
        }
    }
}
