package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.map.EnduranceProgressIndex;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** Creates chunk-local endurance markers and controls their per-player color. */
public final class EnduranceMarkerService implements Listener {
    private static final float BLOCK_SIZE = 0.5F;
    private static final float VIEW_RANGE = 0.5F;
    private static final double TEXT_Y_OFFSET = 0.72D;

    private final BeatPlugin plugin;
    private final AdminAuthorizer admins;
    private final NamespacedKey markerKey;
    private EnduranceProgressIndex competitionIndex;
    private final Map<EnduranceProgressIndex.Entry, CompetitionMarker> competitionMarkers = new HashMap<>();
    private final Map<UUID, Integer> competitionProgress = new HashMap<>();
    private final Map<UUID, SetupSession> setupSessions = new HashMap<>();

    public EnduranceMarkerService(BeatPlugin plugin, AdminAuthorizer admins) {
        this.plugin = plugin;
        this.admins = admins;
        markerKey = new NamespacedKey(plugin, "endurance-progress-marker");
        cleanupLoadedMarkers();
    }

    public void startCompetition(EnduranceProgressIndex index) {
        stopCompetition();
        clearSetupSessions();
        competitionIndex = index;
        index.entries().forEach(this::spawnCompetitionIfLoaded);
    }

    public void showCompetition(Player player, int currentProgress) {
        competitionProgress.put(player.getUniqueId(), currentProgress);
        competitionMarkers.forEach((entry, marker) -> marker.show(player, state(entry.progress(), currentProgress)));
    }

    public void updateCompetition(Player player, int currentProgress) {
        Integer previous = competitionProgress.put(player.getUniqueId(), currentProgress);
        if (previous == null) {
            showCompetition(player, currentProgress);
            return;
        }
        competitionMarkers.forEach((entry, marker) -> {
            MarkerState before = state(entry.progress(), previous);
            MarkerState after = state(entry.progress(), currentProgress);
            if (before != after) marker.switchState(player, before, after);
        });
    }

    public void hideCompetition(Player player) {
        competitionProgress.remove(player.getUniqueId());
        competitionMarkers.values().forEach(marker -> marker.hide(player));
    }

    public void stopCompetition() {
        competitionMarkers.values().forEach(CompetitionMarker::remove);
        competitionMarkers.clear();
        competitionProgress.clear();
        competitionIndex = null;
    }

    public int showSetup(Player player,
            Supplier<Map<Integer, List<EnduranceProgressPoint>>> progressSource,
            boolean persistent, long displayTicks) {
        hideSetup(player);
        SetupSession session = new SetupSession(progressSource);
        setupSessions.put(player.getUniqueId(), session);
        refreshSetup(player, session);
        if (persistent) {
            session.task = Bukkit.getScheduler().runTaskTimer(
                    plugin, () -> refreshSetup(player, session), 20, 20);
        } else {
            session.task = Bukkit.getScheduler().runTaskLater(plugin, () -> hideSetup(player), displayTicks);
        }
        return (int) session.index.entries().stream()
                .filter(entry -> entry.point().world().equals(player.getWorld().getName()))
                .count();
    }

    public boolean hideSetup(Player player) {
        SetupSession session = setupSessions.remove(player.getUniqueId());
        if (session == null) return false;
        if (session.task != null) session.task.cancel();
        session.markers.values().forEach(SetupMarker::remove);
        session.markers.clear();
        return true;
    }

    public void shutdown() {
        stopCompetition();
        setupSessions.values().forEach(session -> {
            if (session.task != null) session.task.cancel();
            session.markers.values().forEach(SetupMarker::remove);
        });
        setupSessions.clear();
        cleanupLoadedMarkers();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (competitionIndex == null || !admins.isAdmin(event.getPlayer().getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (competitionIndex != null && event.getPlayer().isOnline()
                    && admins.isAdmin(event.getPlayer().getUniqueId())) {
                competitionMarkers.values().forEach(marker -> marker.show(event.getPlayer(), MarkerState.FUTURE));
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (competitionIndex != null) {
            competitionIndex.inChunk(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ())
                    .forEach(this::spawnCompetitionIfLoaded);
        }
        setupSessions.forEach((playerId, session) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) return;
            session.index.inChunk(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ())
                    .forEach(entry -> spawnSetupIfLoaded(player, session, entry));
        });
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        var key = new EnduranceProgressIndex.ChunkKey(
                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
        competitionMarkers.entrySet().removeIf(marker -> {
            if (!marker.getKey().chunk().equals(key)) return false;
            marker.getValue().remove();
            return true;
        });
        setupSessions.values().forEach(session -> session.markers.entrySet().removeIf(marker -> {
            if (!marker.getKey().chunk().equals(key)) return false;
            marker.getValue().remove();
            return true;
        }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        competitionProgress.remove(event.getPlayer().getUniqueId());
        hideSetup(event.getPlayer());
    }

    private void spawnCompetitionIfLoaded(EnduranceProgressIndex.Entry entry) {
        if (competitionMarkers.containsKey(entry)) return;
        World world = Bukkit.getWorld(entry.point().world());
        if (world == null || !world.isChunkLoaded(entry.chunk().x(), entry.chunk().z())) return;
        CompetitionMarker marker = new CompetitionMarker(
                text(world, entry),
                block(world, entry, Material.LIME_WOOL),
                block(world, entry, Material.YELLOW_WOOL),
                block(world, entry, Material.LIGHT_BLUE_WOOL));
        competitionMarkers.put(entry, marker);
        competitionProgress.forEach((playerId, progress) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) marker.show(player, state(entry.progress(), progress));
        });
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (admins.isAdmin(player.getUniqueId())) marker.show(player, MarkerState.FUTURE);
        }
    }

    private void spawnSetupIfLoaded(Player player, SetupSession session, EnduranceProgressIndex.Entry entry) {
        if (session.markers.containsKey(entry)) return;
        World world = Bukkit.getWorld(entry.point().world());
        if (world == null || !world.isChunkLoaded(entry.chunk().x(), entry.chunk().z())) return;
        SetupMarker marker = new SetupMarker(text(world, entry), block(world, entry, Material.YELLOW_WOOL));
        session.markers.put(entry, marker);
        marker.show(player);
    }

    private void refreshSetup(Player player, SetupSession session) {
        if (!player.isOnline()) {
            hideSetup(player);
            return;
        }
        EnduranceProgressIndex next = new EnduranceProgressIndex(session.source.get());
        session.markers.entrySet().removeIf(marker -> {
            if (next.entries().contains(marker.getKey())) return false;
            marker.getValue().remove();
            return true;
        });
        session.index = next;
        next.entries().forEach(entry -> spawnSetupIfLoaded(player, session, entry));
    }

    private void clearSetupSessions() {
        for (SetupSession session : setupSessions.values()) {
            if (session.task != null) session.task.cancel();
            session.markers.values().forEach(SetupMarker::remove);
        }
        setupSessions.clear();
    }

    private TextDisplay text(World world, EnduranceProgressIndex.Entry entry) {
        var point = entry.point();
        return world.spawn(new Location(world, point.x(), point.y() + TEXT_Y_OFFSET, point.z()),
                TextDisplay.class, display -> {
                    prepare(display);
                    display.setText(ChatColor.WHITE + "P%03d".formatted(entry.progress()));
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setShadowed(true);
                    display.setSeeThrough(false);
                    display.setDefaultBackground(false);
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                });
    }

    private BlockDisplay block(World world, EnduranceProgressIndex.Entry entry, Material material) {
        var point = entry.point();
        return world.spawn(new Location(world, point.x() - BLOCK_SIZE / 2D, point.y(), point.z() - BLOCK_SIZE / 2D),
                BlockDisplay.class, display -> {
                    prepare(display);
                    display.setBlock(material.createBlockData());
                    display.setTransformation(new Transformation(
                            new Vector3f(), new AxisAngle4f(),
                            new Vector3f(BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE), new AxisAngle4f()));
                });
    }

    private void prepare(Display display) {
        display.setPersistent(false);
        display.setGravity(false);
        display.setInvulnerable(true);
        display.setSilent(true);
        display.setVisibleByDefault(false);
        display.setViewRange(VIEW_RANGE);
        display.setBrightness(new Display.Brightness(15, 15));
        display.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
    }

    private MarkerState state(int markerProgress, int currentProgress) {
        if (markerProgress <= currentProgress) return MarkerState.COMPLETED;
        Integer next = competitionIndex == null ? null : competitionIndex.nextProgress(currentProgress);
        return Integer.valueOf(markerProgress).equals(next) ? MarkerState.NEXT : MarkerState.FUTURE;
    }

    private void cleanupLoadedMarkers() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) entity.remove();
            }
        }
    }

    private enum MarkerState { COMPLETED, NEXT, FUTURE }

    private static final class CompetitionMarker {
        private final TextDisplay text;
        private final BlockDisplay completed;
        private final BlockDisplay next;
        private final BlockDisplay future;

        private CompetitionMarker(TextDisplay text, BlockDisplay completed, BlockDisplay next, BlockDisplay future) {
            this.text = text;
            this.completed = completed;
            this.next = next;
            this.future = future;
        }

        private void show(Player player, MarkerState state) {
            player.showEntity(BeatPlugin.getPlugin(BeatPlugin.class), text);
            player.showEntity(BeatPlugin.getPlugin(BeatPlugin.class), selected(state));
        }

        private void switchState(Player player, MarkerState before, MarkerState after) {
            player.hideEntity(BeatPlugin.getPlugin(BeatPlugin.class), selected(before));
            player.showEntity(BeatPlugin.getPlugin(BeatPlugin.class), selected(after));
        }

        private void hide(Player player) {
            var plugin = BeatPlugin.getPlugin(BeatPlugin.class);
            player.hideEntity(plugin, text);
            player.hideEntity(plugin, completed);
            player.hideEntity(plugin, next);
            player.hideEntity(plugin, future);
        }

        private BlockDisplay selected(MarkerState state) {
            return switch (state) {
                case COMPLETED -> completed;
                case NEXT -> next;
                case FUTURE -> future;
            };
        }

        private void remove() {
            text.remove();
            completed.remove();
            next.remove();
            future.remove();
        }
    }

    private static final class SetupMarker {
        private final TextDisplay text;
        private final BlockDisplay block;

        private SetupMarker(TextDisplay text, BlockDisplay block) {
            this.text = text;
            this.block = block;
        }

        private void show(Player player) {
            var plugin = BeatPlugin.getPlugin(BeatPlugin.class);
            player.showEntity(plugin, text);
            player.showEntity(plugin, block);
        }

        private void remove() {
            text.remove();
            block.remove();
        }
    }

    private static final class SetupSession {
        private final Supplier<Map<Integer, List<EnduranceProgressPoint>>> source;
        private EnduranceProgressIndex index;
        private final Map<EnduranceProgressIndex.Entry, SetupMarker> markers = new HashMap<>();
        private BukkitTask task;

        private SetupSession(Supplier<Map<Integer, List<EnduranceProgressPoint>>> source) {
            this.source = source;
            this.index = new EnduranceProgressIndex(source.get());
        }
    }
}
