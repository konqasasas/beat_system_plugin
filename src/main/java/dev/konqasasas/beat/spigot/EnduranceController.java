package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.EnduranceResultService;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.domain.endurance.EnduranceFallPolicy;
import dev.konqasasas.beat.domain.endurance.EnduranceRecord;
import dev.konqasasas.beat.domain.endurance.EnduranceRules;
import dev.konqasasas.beat.domain.endurance.EnduranceSession;
import dev.konqasasas.beat.domain.ranking.RankedEntry;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.map.EnduranceProgressIndex;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.map.validation.MapValidationService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RosterService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class EnduranceController implements Listener, LiveCompetitionClock {
    private final BeatPlugin plugin;
    private final RosterService rosters;
    private final EventStateService states;
    private final MapConfigurationService maps;
    private final MapValidationService validation;
    private final EnduranceResultService results;
    private final ConfigurationFiles configuration;
    private final CompetitionSettingsService settings;
    private final EnduranceMarkerService markers;
    private final Set<Integer> processedEliminations = new HashSet<>();
    private final Set<UUID> suppressTeleportDetection = new HashSet<>();
    private final ActionBarFeedbackState feedback = new ActionBarFeedbackState();
    private final Map<UUID, String> renderedTabRows = new HashMap<>();

    private EnduranceSession session;
    private EnduranceProgressIndex progressIndex;
    private BukkitTask task;
    private BossBar bar;
    private SharedRankingBoard rankingBoard;
    private int countdown;
    private long tick;
    private long totalTicks;
    private boolean displayDirty;
    private List<Integer> eliminationTicks = List.of();

    public EnduranceController(BeatPlugin plugin, RosterService rosters, EventStateService states,
            MapConfigurationService maps, MapValidationService validation, EnduranceResultService results,
            ConfigurationFiles configuration, CompetitionSettingsService settings,
            EnduranceMarkerService markers) {
        this.plugin = plugin;
        this.rosters = rosters;
        this.states = states;
        this.maps = maps;
        this.validation = validation;
        this.results = results;
        this.configuration = configuration;
        this.settings = settings;
        this.markers = markers;
    }

    public void start() throws PersistenceException {
        if (session != null) throw new IllegalStateException("耐久は進行中です");
        TournamentState state = states.current().tournamentState();
        if (state != TournamentState.TA_FINISHED && state != TournamentState.ENDURANCE_READY) {
            throw new IllegalStateException("耐久を開始できる状態ではありません");
        }
        var report = validation.validateEndurance(maps.endurance());
        if (!report.passed()) throw new IllegalStateException("耐久マップ検証ERROR: " + report.errorCount());
        var schedule = settings.current();
        totalTicks = schedule.enduranceRunningTicks();
        eliminationTicks = schedule.enduranceEliminationTicks();
        processedEliminations.clear();
        if (state == TournamentState.TA_FINISHED) states.transitionTo(TournamentState.ENDURANCE_READY);
        states.transitionTo(TournamentState.ENDURANCE_COUNTDOWN);
        Map<UUID, String> names = new LinkedHashMap<>();
        rosters.current().participants().forEach((id, identity) ->
                names.put(id, states.tournamentName(id).orElse(identity.mcid())));
        var config = maps.endurance();
        session = new EnduranceSession(names,
                new EnduranceRules(config.zone2Progress(), config.zone3Progress(), config.goalProgress()));
        progressIndex = new EnduranceProgressIndex(config.progresses());
        bar = Bukkit.createBossBar(
                configuration.message("ui.bossbar.endurance-title", "ENDURANCE"),
                configuration.barColor("boss-bars.endurance", BarColor.GREEN),
                configuration.barStyle("boss-bars.endurance", BarStyle.SOLID));
        rankingBoard = new SharedRankingBoard(
                "beat_end", configuration.message("ui.scoreboard.endurance-title", "ENDURANCE"));
        countdown = (int) schedule.startCountdownTicks();
        tick = 0;
        displayDirty = true;
        renderedTabRows.clear();
        forPlayers(this::prepare);
        announce(countdown / 20);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void move(PlayerMoveEvent event) {
        if (session == null || countdown <= 0 || !session.contains(event.getPlayer().getUniqueId())
                || event.getTo() == null) return;
        var from = event.getFrom();
        var to = event.getTo();
        to.setX(from.getX());
        to.setY(from.getY());
        to.setZ(from.getZ());
        event.setTo(to);
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        if (session == null || !session.contains(event.getPlayer().getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            rankingBoard.show(player);
            if (session.active(player.getUniqueId())) {
                CompetitionPlayerState.normalize(player);
                bar.addPlayer(player);
                if (countdown == 0) markers.showCompetition(
                        player, session.record(player.getUniqueId()).maxProgress());
                displayDirty = true;
                return;
            }
            if (!eliminationTicks.isEmpty() && tick >= eliminationTicks.getFirst()) {
                player.setGameMode(GameMode.SPECTATOR);
                displayDirty = true;
                return;
            }
            session.activate(player.getUniqueId());
            CompetitionPlayerState.normalize(player);
            teleport(player, maps.endurance().start());
            bar.addPlayer(player);
            if (countdown == 0) markers.showCompetition(player, 0);
            displayDirty = true;
        });
    }

    private void run() {
        try {
            if (countdown > 0) {
                if (--countdown == 0) {
                    states.transitionTo(TournamentState.ENDURANCE_RUNNING);
                    markers.startCompetition(progressIndex);
                    forPlayers(player -> {
                        session.activate(player.getUniqueId());
                        markers.showCompetition(player, session.record(player.getUniqueId()).maxProgress());
                        playConfigured(player, "sounds.competition-start",
                                Sound.BLOCK_NOTE_BLOCK_BELL, 1F, 1.2F);
                    });
                    Bukkit.broadcastMessage(configuration.message(
                            "notifications.endurance.started", "[BEAT] 耐久競技開始！"));
                    displayDirty = true;
                    updateDisplayIfDirty();
                    updateBossBar();
                } else if (countdown % 20 == 0) {
                    announce(countdown / 20);
                }
                return;
            }
            long previous = tick++;
            scan();
            cutoffs(previous, tick);
            announceTimeLimit();
            updateDisplayIfDirty();
            updateUi();
            if (tick >= totalTicks) finish();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "耐久進行を停止しました", exception);
            shutdown();
        }
    }

    private void scan() {
        double horizontalRadius = configuration.configDouble(
                "endurance.progress-trigger.horizontal-radius", 0.25D, 0D, 16D);
        double verticalTolerance = configuration.configDouble(
                "endurance.progress-trigger.vertical-tolerance", 0.5D, 0D, 16D);
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (!session.contains(id) || !session.active(id)) continue;
            if (suppressTeleportDetection.remove(id)) continue;
            MapLocation location = location(player);
            int best = progressIndex.highestContaining(location, horizontalRadius, verticalTolerance);
            if (best > 0) updateProgress(player, best);
            if (EnduranceFallPolicy.shouldRestart(
                    session.record(id), player.getLocation().getY(), maps.endurance().fallY())) {
                player.setVelocity(new Vector());
                teleport(player, restart(id));
                suppressTeleportDetection.add(id);
            }
        }
    }

    private void updateProgress(Player player, int progress) {
        UUID id = player.getUniqueId();
        var update = session.reach(id, progress, tick);
        if (!update.progress().changed()) return;
        displayDirty = true;
        markers.updateCompetition(player, update.progress().currentProgress());
        if (update.goal()) {
            notice(player, configuration.message(
                    "notifications.endurance.goal", "GOAL! ｜ {time} ｜ #{rank}",
                    Map.of("time", clock(tick), "rank", update.currentRank())));
            Bukkit.broadcastMessage(configuration.message(
                    "notifications.endurance.goal-broadcast", "[BEAT] {player} が完走！ {time} (#{rank})",
                    Map.of("player", session.competitor(id).tournamentName(),
                            "time", clock(tick), "rank", update.currentRank())));
            playConfigured(player, "sounds.endurance-goal", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        } else if (update.zone2() || update.zone3()) {
            int zone = update.zone3() ? 3 : 2;
            notice(player, configuration.message(
                    "notifications.endurance.zone", "Zone {zone} 到達 ｜ Progress {progress} ｜ #{rank}",
                    Map.of("zone", zone, "progress", "%03d".formatted(progress),
                            "rank", "%02d".formatted(update.currentRank()))));
            playConfigured(player, "sounds.endurance-zone", Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        } else {
            notice(player, configuration.message(
                    "notifications.endurance.progress", "Progress {progress} 到達 ｜ #{rank}",
                    Map.of("progress", "%03d".formatted(progress),
                            "rank", "%02d".formatted(update.currentRank()))));
            playConfigured(player, "sounds.endurance-progress", Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, 1.5F);
        }
        if (update.rankChanged() && !update.goal()) {
            Bukkit.broadcastMessage(configuration.message(
                    "notifications.endurance.rank-update",
                    "[BEAT] {player} が更新: Progress {progress} (#{rank})",
                    Map.of("player", session.competitor(id).tournamentName(),
                            "progress", "%03d".formatted(progress), "rank", update.currentRank())));
        }
    }

    private MapLocation restart(UUID id) {
        return switch (session.zone(id)) {
            case 3 -> maps.endurance().zone3Restart();
            case 2 -> maps.endurance().zone2Restart();
            default -> maps.endurance().start();
        };
    }

    private void cutoffs(long previous, long current) {
        for (int index = 0; index < Math.min(2, eliminationTicks.size()); index++) {
            long boundary = eliminationTicks.get(index);
            long remaining = boundary - current;
            int zone = index + 2;
            if (remaining >= 20 && remaining <= 200 && remaining % 20 == 0) {
                forPlayers(player -> {
                    player.sendMessage(configuration.message(
                            "notifications.endurance.elimination-warning",
                            "[BEAT] Zone {zone} 未到達者脱落まで... {seconds}",
                            Map.of("zone", zone, "seconds", remaining / 20)));
                    playConfigured(player, "sounds.countdown", Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                });
            }
            if (previous < boundary && current >= boundary && processedEliminations.add(index)) {
                Bukkit.broadcastMessage(configuration.message(
                        "notifications.endurance.elimination", "[BEAT] Zone {zone} 未到達者脱落！",
                        Map.of("zone", zone)));
                forPlayers(player -> playConfigured(
                        player, "sounds.elimination", Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.7F));
                var eliminated = session.eliminateWithoutZone(zone);
                displayDirty = true;
                for (var result : eliminated) {
                    Player player = Bukkit.getPlayer(result.id());
                    if (player == null) continue;
                    markers.hideCompetition(player);
                    player.setGameMode(GameMode.SPECTATOR);
                    player.sendMessage(configuration.message(
                            "notifications.endurance.eliminated",
                            "[BEAT] 脱落しました。最終記録: Progress {progress} (#{rank})",
                            Map.of("progress", "%03d".formatted(result.progress()), "rank", result.rank())));
                }
            }
        }
    }

    private void announceTimeLimit() {
        long remaining = totalTicks - tick;
        if (remaining > 0 && remaining <= 200 && remaining % 20 == 0) {
            forPlayers(player -> {
                player.sendMessage(configuration.message(
                        "notifications.endurance.time-limit-countdown",
                        "[BEAT] 制限時間終了まで... {seconds}", Map.of("seconds", remaining / 20)));
                playConfigured(player, "sounds.countdown", Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
            });
        } else if (remaining == 0) {
            forPlayers(player -> {
                player.sendMessage(configuration.message(
                        "notifications.endurance.time-limit-ended", "[BEAT] 制限時間終了！"));
                playConfigured(player, "sounds.time-limit-end", Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5F);
            });
        }
    }

    private void updateUi() {
        if (tick % configuration.configInt("ui-update-ticks.boss-bar", 10, 1, 1200) == 0) updateBossBar();
        if (tick % configuration.configInt("ui-update-ticks.action-bar", 10, 1, 1200) != 0) return;
        Map<UUID, Integer> ranks = rankMap(session.rankings());
        forPlayers(player -> {
            UUID id = player.getUniqueId();
            String notification = feedback.activeMessage(id, tick);
            if (notification != null) {
                action(player, notification);
                return;
            }
            int progress = session.record(id).maxProgress();
            String rank = progress == 0 ? "--" : "%02d".formatted(ranks.get(id));
            action(player, configuration.message(
                    "ui.endurance.status", "Progress {progress} ｜ #{rank} ｜ Zone {zone}",
                    Map.of("progress", "%03d".formatted(progress), "rank", rank,
                            "zone", "%02d".formatted(session.zone(id)))));
        });
    }

    private void updateBossBar() {
        bar.setProgress(Math.max(0, (double) (totalTicks - tick) / totalTicks));
        Long next = eliminationTicks.stream().mapToLong(Integer::longValue)
                .filter(value -> value > tick).boxed().findFirst().orElse(null);
        bar.setTitle(configuration.message(
                "ui.bossbar.competition-time", "残り時間 {remaining} ｜ 次の脱落 {next}",
                Map.of("remaining", clock(totalTicks - tick),
                        "next", next == null ? "--:--" : clock(next - tick))));
    }

    private void updateDisplayIfDirty() {
        if (!displayDirty) return;
        displayDirty = false;
        var ranking = session.rankings();
        List<String> rows = ranking.stream().limit(10).map(entry -> configuration.message(
                "ui.scoreboard.endurance-row", "#{rank} Progress {progress} {player}",
                Map.of("rank", "%02d".formatted(entry.rank()),
                        "progress", "%03d".formatted(entry.record().maxProgress()),
                        "player", entry.competitor().tournamentName()))).toList();
        rankingBoard.update(rows);
        forPlayers(rankingBoard::show);
        for (var entry : ranking) {
            Player player = Bukkit.getPlayer(entry.competitor().uuid());
            if (player == null) continue;
            String row = configuration.message(
                    "ui.tab.endurance-row", "#{rank} Progress {progress} {player}",
                    Map.of("rank", "%02d".formatted(entry.rank()),
                            "progress", "%03d".formatted(entry.record().maxProgress()),
                            "player", player.getName()));
            if (!row.equals(renderedTabRows.put(player.getUniqueId(), row))) {
                player.setPlayerListOrder(entry.rank());
                player.setPlayerListName(row);
            }
        }
    }

    private static Map<UUID, Integer> rankMap(List<RankedEntry<EnduranceRecord>> ranking) {
        Map<UUID, Integer> result = new HashMap<>();
        ranking.forEach(entry -> result.put(entry.competitor().uuid(), entry.rank()));
        return result;
    }

    @Override
    public synchronized Optional<LiveCompetitionClock.Snapshot> liveClock() {
        if (session == null) return Optional.empty();
        return Optional.of(new LiveCompetitionClock.Snapshot(
                countdown > 0 ? "ENDURANCE_COUNTDOWN" : "ENDURANCE_RUNNING",
                countdown > 0 ? settings.current().startCountdownTicks() - countdown : tick,
                countdown > 0 ? settings.current().startCountdownTicks() : totalTicks,
                countdown > 0 ? List.of(settings.current().startCountdownTicks())
                        : eliminationTicks.stream().map(Integer::longValue).toList()));
    }

    @Override
    public synchronized void debugSetElapsedTick(long target) {
        if (session == null) throw new IllegalStateException("耐久は進行中ではありません");
        if (countdown > 0) {
            long elapsed = settings.current().startCountdownTicks() - countdown;
            if (target < elapsed || target >= settings.current().startCountdownTicks()) {
                throw new IllegalArgumentException("指定時刻が範囲外です");
            }
            countdown = (int) (settings.current().startCountdownTicks() - target);
            return;
        }
        if (target < tick || target >= totalTicks) throw new IllegalArgumentException("指定時刻が範囲外です");
        long previous = tick;
        tick = target;
        cutoffs(previous, tick);
        displayDirty = true;
        updateDisplayIfDirty();
        updateBossBar();
        updateUi();
    }

    public void forceFinish() throws PersistenceException {
        if (session == null) throw new IllegalStateException("耐久競技は実行中ではありません");
        finish();
    }

    private void finish() throws PersistenceException {
        session.finish();
        results.saveFinal(session);
        states.transitionTo(TournamentState.ENDURANCE_FINISHED);
        forPlayers(player -> {
            player.setGameMode(GameMode.ADVENTURE);
            player.setVelocity(new Vector());
            teleport(player, maps.endurance().end());
        });
        Bukkit.broadcastMessage(configuration.message(
                "notifications.endurance.finished", "[BEAT] 耐久が終了し結果を確定しました。"));
        shutdown();
    }

    private void prepare(Player player) {
        CompetitionPlayerState.normalize(player);
        teleport(player, maps.endurance().start());
        bar.addPlayer(player);
        rankingBoard.show(player);
    }

    private void announce(int seconds) {
        forPlayers(player -> {
            player.sendMessage(configuration.message(
                    "notifications.endurance.countdown", "[BEAT] 競技開始まで... {seconds}",
                    Map.of("seconds", seconds)));
            playConfigured(player, "sounds.countdown", Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
        });
    }

    private void playConfigured(Player player, String path, Sound fallback, float volume, float pitch) {
        player.playSound(player.getLocation(), configuration.sound(path, fallback),
                configuration.soundVolume(path, volume), configuration.soundPitch(path, pitch));
    }

    private void forPlayers(java.util.function.Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (session != null && session.contains(player.getUniqueId())) action.accept(player);
        }
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        markers.stopCompetition();
        if (bar != null) bar.removeAll();
        bar = null;
        if (rankingBoard != null) rankingBoard.clear();
        if (session != null) {
            forPlayers(player -> {
                CompetitionPlayerState.release(player);
                player.setPlayerListOrder(0);
                player.setPlayerListName(player.getName());
                var manager = Bukkit.getScoreboardManager();
                if (manager != null) player.setScoreboard(manager.getMainScoreboard());
            });
        }
        session = null;
        progressIndex = null;
        rankingBoard = null;
        displayDirty = false;
        renderedTabRows.clear();
        suppressTeleportDetection.clear();
        feedback.clear();
    }

    private void notice(Player player, String message) {
        String notification = configuration.actionBarNotification(message);
        feedback.show(player.getUniqueId(), notification,
                tick + configuration.styleInt("display-ticks.feedback", 20, 1, 1200));
        action(player, notification);
    }

    private static void action(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }

    private static String clock(long ticks) {
        long seconds = Math.max(0, (ticks + 19) / 20);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }

    private static MapLocation location(Player player) {
        var location = player.getLocation();
        return new MapLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    private static void teleport(Player player, MapLocation location) {
        World world = Bukkit.getWorld(location.world());
        if (world != null) player.teleport(new Location(
                world, location.x(), location.y(), location.z(), location.yaw(), location.pitch()));
    }
}
