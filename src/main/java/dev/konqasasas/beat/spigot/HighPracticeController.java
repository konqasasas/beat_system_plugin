package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.domain.high.HighPracticeSession;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.map.validation.MapValidationService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RosterService;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class HighPracticeController implements Listener, LiveCompetitionClock {
    private final BeatPlugin plugin;
    private final RosterService rosters;
    private final EventStateService eventState;
    private final MapConfigurationService maps;
    private final MapValidationService validation;
    private final HighPracticeItems items;
    private final HighCompetitionController competition;
    private final ConfigurationFiles configuration;
    private final CompetitionSettingsService settings;
    private HighPracticeSession session;
    private BukkitTask task;
    private BossBar phaseBossBar;

    public HighPracticeController(BeatPlugin plugin, RosterService rosters, EventStateService eventState,
            MapConfigurationService maps, MapValidationService validation,
            HighCompetitionController competition, ConfigurationFiles configuration,
            CompetitionSettingsService settings) {
        this.plugin = plugin;
        this.rosters = rosters;
        this.eventState = eventState;
        this.maps = maps;
        this.validation = validation;
        this.items = new HighPracticeItems(plugin);
        this.competition = competition;
        this.configuration = configuration;
        this.settings = settings;
    }

    public synchronized void start() throws PersistenceException {
        if (session != null) throw new IllegalStateException("高難易度フェーズは既に進行中です");
        if (eventState.current().tournamentState() != TournamentState.WAITING) {
            throw new IllegalStateException("大会状態がWAITINGではありません: " + eventState.current().tournamentState());
        }
        var report = validation.validateHigh(maps.high());
        if (!report.passed()) {
            throw new IllegalStateException("マップ検証に失敗しました（ERROR " + report.errorCount() + "件）。/beat setup validate high を確認してください");
        }
        Set<UUID> participants = rosters.current().participants().keySet().stream().collect(Collectors.toUnmodifiableSet());
        if (participants.isEmpty()) throw new IllegalStateException("参加者が登録されていません");
        var schedule = settings.current();
        long practiceTicks = schedule.highPracticeTicks();
        long prepareTicks = schedule.highPrepareTicks();
        long countdownTicks = schedule.startCountdownTicks();
        HighPracticeSession candidate =
                new HighPracticeSession(participants, countdownTicks, practiceTicks, prepareTicks);
        eventState.transitionTo(TournamentState.HIGH_PRACTICE_COUNTDOWN);
        session = candidate;
        forOnlineParticipants(this::enterCountdown);
        announceCountdown(10, "notifications.high.practice-countdown", "[BEAT] 練習開始まで... {seconds}");
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public synchronized boolean active() { return session != null; }

    @Override
    public synchronized java.util.Optional<LiveCompetitionClock.Snapshot> liveClock() {
        if (session == null || session.phase() == HighPracticeSession.Phase.COMPLETE) return java.util.Optional.empty();
        return java.util.Optional.of(new LiveCompetitionClock.Snapshot(
                "HIGH_" + session.phase(), session.phaseTick(), session.phaseDurationTicks(),
                java.util.List.of(session.phaseDurationTicks())));
    }

    @Override
    public synchronized void debugSetElapsedTick(long targetTick) throws PersistenceException {
        if (session == null) throw new IllegalStateException("高難易度フェーズは進行中ではありません");
        HighPracticeSession.Phase phase = session.phase();
        if (targetTick < session.phaseTick()) throw new IllegalArgumentException("競技時間は巻き戻せません");
        while (session != null && session.phase() == phase && session.phaseTick() < targetTick) tick();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        HighPracticeSession current = session;
        if (current == null || current.phase() != HighPracticeSession.Phase.COUNTDOWN
                || !current.contains(event.getPlayer().getUniqueId()) || event.getTo() == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            to.setX(from.getX()); to.setY(from.getY()); to.setZ(from.getZ());
            event.setTo(to);
        }
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        HighPracticeItems.Kind kind = items.kind(event.getItem());
        if (kind == null) return;
        event.setCancelled(true);
        HighPracticeSession current = session;
        Player player = event.getPlayer();
        if (current == null || current.phase() != HighPracticeSession.Phase.PRACTICE
                || !current.contains(player.getUniqueId())) return;
        if (kind == HighPracticeItems.Kind.FLIGHT) {
            boolean enabled = current.toggleFlight(player.getUniqueId());
            player.setAllowFlight(enabled);
            if (!enabled) player.setFlying(false);
            actionBar(player, "飛行: " + (enabled ? "ON" : "OFF"));
            playConfigured(player, "sounds.high-practice-flight", Sound.BLOCK_NOTE_BLOCK_PLING,
                    0.8F, enabled ? 1.4F : 0.8F);
        } else {
            current.checkpoint(player.getUniqueId()).ifPresentOrElse(checkpoint -> {
                boolean flying = player.isFlying();
                player.setVelocity(new Vector());
                teleport(player, checkpoint);
                if (player.getAllowFlight()) player.setFlying(flying);
            }, () -> player.sendMessage(configuration.message(
                    "high.practice.checkpoint-missing",
                    "[BEAT] CPが設定されていません。Qで設定してください。")));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        HighPracticeItems.Kind kind = items.kind(event.getItemDrop().getItemStack());
        if (kind == null) return;
        HighPracticeSession current = session;
        Player player = event.getPlayer();
        if (current == null || current.phase() != HighPracticeSession.Phase.PRACTICE
                || !current.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        if (kind != HighPracticeItems.Kind.CHECKPOINT) return;
        boolean saved = current.setCheckpoint(
                player.getUniqueId(), mapLocation(player), ((org.bukkit.entity.Entity) player).isOnGround());
        player.sendMessage(saved
                ? configuration.message("high.practice.checkpoint-set", "[BEAT] CPを設定しました")
                : configuration.message(
                        "high.practice.checkpoint-ground-only",
                        "[BEAT] 地面にいるときのみCPを設定できます"));
        if (saved) playConfigured(player, "sounds.high-practice-checkpoint",
                Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.8F, 1.2F);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        HighPracticeSession current = session;
        if (current == null || !current.contains(event.getPlayer().getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> restoreForCurrentPhase(event.getPlayer()));
    }

    public synchronized void shutdown() {
        if (task != null) task.cancel();
        task = null;
        removeBossBar();
        forOnlineParticipants(player -> { cleanupPracticePlayer(player); CompetitionPlayerState.release(player); });
        session = null;
    }

    public synchronized void skipPractice() throws PersistenceException {
        if (session == null || session.phase() != HighPracticeSession.Phase.PRACTICE) {
            throw new IllegalStateException("高難易度の練習フェーズ中ではありません");
        }
        session.skipPractice();
        removeBossBar();
        enterPrepare();
    }

    private synchronized void tick() {
        if (session == null) return;
        HighPracticeSession.Phase before = session.phase();
        HighPracticeSession.Phase after = session.advanceOneTick();
        try {
            if (before == HighPracticeSession.Phase.COUNTDOWN) tickCountdown(after);
            else if (before == HighPracticeSession.Phase.PRACTICE) tickPractice(after);
            else if (before == HighPracticeSession.Phase.PREPARE) tickPrepare(after);
        } catch (PersistenceException exception) {
            plugin.getLogger().log(Level.SEVERE, "高難易度フェーズ状態を保存できないため進行を停止します", exception);
            shutdown();
        }
    }

    private void tickCountdown(HighPracticeSession.Phase after) throws PersistenceException {
        if (after == HighPracticeSession.Phase.PRACTICE) {
            eventState.transitionTo(TournamentState.HIGH_PRACTICE);
            phaseBossBar = Bukkit.createBossBar(
                    "練習",
                    configuration.barColor("boss-bars.high-practice", BarColor.GREEN),
                    configuration.barStyle("boss-bars.high-practice", BarStyle.SOLID));
            forOnlineParticipants(player -> {
                items.give(player);
                phaseBossBar.addPlayer(player);
                player.sendMessage(configuration.message(
                        "notifications.high.practice-started",
                        "[BEAT] 高難易度の練習を開始します。"));
                playConfigured(player, "sounds.competition-start",
                        Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1.2F);
            });
            updatePracticeBossBar();
            announcePracticeEndingIfDue();
            return;
        }
        long remaining = session.remainingTicks();
        if (remaining > 0 && remaining % 20 == 0) {
            announceCountdown(
                    (int) (remaining / 20),
                    "notifications.high.practice-countdown",
                    "[BEAT] 練習開始まで... {seconds}");
        }
    }

    private void tickPractice(HighPracticeSession.Phase after) throws PersistenceException {
        if (after == HighPracticeSession.Phase.PREPARE) {
            removeBossBar();
            enterPrepare();
            return;
        }
        if (bossBarUpdateDue()) updatePracticeBossBar();
        announcePracticeEndingIfDue();
    }

    private void updatePracticeBossBar() {
        if (phaseBossBar == null) return;
        long remaining = session.remainingTicks();
        long total = session.phaseDurationTicks();
        phaseBossBar.setProgress(Math.max(0D, Math.min(1D, (double) remaining / total)));
        phaseBossBar.setTitle(configuration.message(
                "ui.bossbar.high-practice",
                "練習 残り時間 {remaining}",
                java.util.Map.of("remaining", formatTicks(remaining))));
    }

    private void announcePracticeEndingIfDue() {
        long remaining = session.remainingTicks();
        if (remaining > 0 && remaining <= 200 && remaining % 20 == 0) {
            announceCountdown(
                    (int) (remaining / 20),
                    "notifications.high.practice-ending-countdown",
                    "[BEAT] 練習終了まで... {seconds}");
        }
    }

    private void enterPrepare() throws PersistenceException {
        eventState.transitionTo(TournamentState.HIGH_PREPARE);
        phaseBossBar = Bukkit.createBossBar(
                "準備",
                configuration.barColor("boss-bars.high-prepare", BarColor.YELLOW),
                configuration.barStyle("boss-bars.high-prepare", BarStyle.SOLID));
        forOnlineParticipants(player -> {
            cleanupPracticePlayer(player);
            teleport(player, maps.high().prepare());
            phaseBossBar.addPlayer(player);
        });
        updatePrepareBossBar();
    }

    private void tickPrepare(HighPracticeSession.Phase after) throws PersistenceException {
        if (after == HighPracticeSession.Phase.COMPLETE) {
            removeBossBar();
            competition.startFromPrepare();
            forOnlineParticipants(player -> {
                player.sendMessage(configuration.message(
                        "notifications.high.running-started",
                        "[BEAT] 高難易度本番を開始します。"));
                playConfigured(player, "sounds.competition-start",
                        Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1.2F);
            });
            if (task != null) task.cancel();
            task = null;
            session = null;
            return;
        }
        if (bossBarUpdateDue()) updatePrepareBossBar();
        long remaining = session.remainingTicks();
        if (remaining > 0 && remaining <= 200 && remaining % 20 == 0) {
            announceCountdown(
                    (int) (remaining / 20),
                    "notifications.high.running-countdown",
                    "[BEAT] 本番開始まで... {seconds}");
        }
    }

    private void updatePrepareBossBar() {
        if (phaseBossBar == null) return;
        long remaining = session.remainingTicks();
        long total = session.phaseDurationTicks();
        phaseBossBar.setProgress(Math.max(0D, Math.min(1D, (double) remaining / total)));
        phaseBossBar.setTitle(configuration.message(
                "ui.bossbar.high-prepare",
                "準備 残り時間 {remaining}",
                java.util.Map.of("remaining", formatTicks(remaining))));
    }

    private boolean bossBarUpdateDue() {
        return session.phaseTick() % configuration.configInt("ui-update-ticks.boss-bar", 10, 1, 1200) == 0;
    }

    private void restoreForCurrentPhase(Player player) {
        switch (session.phase()) {
            case COUNTDOWN -> enterCountdown(player);
            case PRACTICE -> { CompetitionPlayerState.normalize(player); teleport(player, maps.high().courses().get(1).start()); items.give(player); if (phaseBossBar != null) phaseBossBar.addPlayer(player); }
            case PREPARE -> { cleanupPracticePlayer(player); CompetitionPlayerState.normalize(player); teleport(player, maps.high().prepare()); if (phaseBossBar != null) phaseBossBar.addPlayer(player); }
            case COMPLETE -> { }
        }
    }

    private void enterCountdown(Player player) {
        cleanupPracticePlayer(player);
        CompetitionPlayerState.normalize(player);
        teleport(player, maps.high().courses().get(1).start());
    }

    private void cleanupPracticePlayer(Player player) {
        items.remove(player);
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setVelocity(new Vector());
    }

    private void forOnlineParticipants(java.util.function.Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (session != null && session.contains(player.getUniqueId())) action.accept(player);
        }
    }

    private void announceCountdown(int seconds, String path, String fallback) {
        forOnlineParticipants(player -> {
            player.sendMessage(configuration.message(
                    path, fallback, java.util.Map.of("seconds", seconds)));
            String soundPath = "sounds.countdown";
            player.playSound(
                    player.getLocation(),
                    configuration.sound(soundPath, Sound.BLOCK_NOTE_BLOCK_HAT),
                    configuration.soundVolume(soundPath, 1F),
                    configuration.soundPitch(soundPath, 1F));
        });
    }

    private void playConfigured(Player player, String path, Sound fallback, float volume, float pitch) {
        player.playSound(player.getLocation(), configuration.sound(path, fallback),
                configuration.soundVolume(path, volume), configuration.soundPitch(path, pitch));
    }

    private static void actionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }

    private static MapLocation mapLocation(Player player) {
        Location l = player.getLocation();
        return new MapLocation(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    private static void teleport(Player player, MapLocation target) {
        if (target == null) return;
        World world = Bukkit.getWorld(target.world());
        if (world == null) return;
        player.teleport(new Location(world, target.x(), target.y(), target.z(), target.yaw(), target.pitch()));
    }

    private void removeBossBar() {
        if (phaseBossBar != null) phaseBossBar.removeAll();
        phaseBossBar = null;
    }

    private static String formatTicks(long ticks) {
        long seconds = (ticks + 19) / 20;
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }

}
