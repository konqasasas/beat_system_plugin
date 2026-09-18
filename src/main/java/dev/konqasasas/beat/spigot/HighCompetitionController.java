package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.application.HighResultService;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.domain.high.HighCompetitionSession;
import dev.konqasasas.beat.domain.high.HighDifficultyRules;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.map.BlockRegion;
import dev.konqasasas.beat.map.HighCourseMap;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.map.validation.MapValidationService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RosterService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class HighCompetitionController implements Listener, LiveCompetitionClock {
    private final BeatPlugin plugin;
    private final RosterService rosters;
    private final AdminAuthorizer admins;
    private final EventStateService eventState;
    private final MapConfigurationService maps;
    private final HighResultService results;
    private final MapValidationService validation;
    private final HighRunningItem returnItem;
    private final ConfigurationFiles configuration;
    private final CompetitionSettingsService settings;
    private final PlayerCollisionService collisions;
    private HighCompetitionSession session;
    private HighCompetitionDisplay display;
    private BukkitTask task;
    private long elapsedTick;
    private long totalTicks;
    private boolean rankingDirty;
    private List<Integer> eliminationTicks = List.of();
    private final java.util.Set<Integer> processedEliminations = new java.util.HashSet<>();

    public HighCompetitionController(BeatPlugin plugin, RosterService rosters, AdminAuthorizer admins,
            EventStateService eventState, MapConfigurationService maps, HighResultService results,
            MapValidationService validation, ConfigurationFiles configuration,
            CompetitionSettingsService settings, PlayerCollisionService collisions) {
        this.plugin = plugin;
        this.rosters = rosters;
        this.admins = admins;
        this.eventState = eventState;
        this.maps = maps;
        this.results = results;
        this.validation = validation;
        this.returnItem = new HighRunningItem(plugin);
        this.configuration = configuration;
        this.settings = settings;
        this.collisions = collisions;
    }

    public synchronized void startFromPrepare() throws PersistenceException {
        if (session != null) throw new IllegalStateException("高難易度本番は既に進行中です");
        if (eventState.current().tournamentState() != TournamentState.HIGH_PREPARE) {
            throw new IllegalStateException("大会状態がHIGH_PREPAREではありません");
        }
        var validationReport = validation.validateHigh(maps.high());
        if (!validationReport.passed()) {
            throw new IllegalStateException("マップ検証に失敗しました（ERROR "
                    + validationReport.errorCount() + "件）");
        }
        Map<Integer, Integer> spotCounts = new LinkedHashMap<>();
        for (int course = 1; course <= 5; course++) {
            spotCounts.put(course, maps.high().courses().get(course).spots().size());
        }
        Map<UUID, String> names = new LinkedHashMap<>();
        rosters.current().participants().forEach((uuid, identity) -> names.put(
                uuid, eventState.tournamentName(uuid).orElse(identity.mcid())));
        HighCompetitionSession candidate = new HighCompetitionSession(names, new HighDifficultyRules(
                spotCounts,
                plugin.getConfig().getInt("high-difficulty.spot-points", 50),
                plugin.getConfig().getInt("high-difficulty.goal-points", 100)));
        var schedule = settings.current();
        long configuredTotal = schedule.highRunningTicks();
        List<Integer> configuredEliminations = schedule.highEliminationTicks();
        eventState.transitionTo(TournamentState.HIGH_RUNNING);
        session = candidate;
        totalTicks = configuredTotal;
        eliminationTicks = List.copyOf(configuredEliminations);
        elapsedTick = 0;
        rankingDirty = true;
        processedEliminations.clear();
        display = new HighCompetitionDisplay(configuration, collisions);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (session.contains(player.getUniqueId())) activateAtStart(player);
            else if (admins.isAdmin(player.getUniqueId())) display.add(player);
        }
        var next = nextElimination();
        display.updateImmediately(elapsedTick, totalTicks, next == null ? null : next.tick(),
                next == null ? null : next.requiredCourse(), session);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public synchronized boolean active() { return session != null; }

    @Override
    public synchronized java.util.Optional<LiveCompetitionClock.Snapshot> liveClock() {
        return session == null ? java.util.Optional.empty() : java.util.Optional.of(
                new LiveCompetitionClock.Snapshot("HIGH_RUNNING", elapsedTick, totalTicks,
                        eliminationTicks.stream().map(Integer::longValue).toList()));
    }

    @Override
    public synchronized void debugSetElapsedTick(long targetTick) {
        if (session == null) throw new IllegalStateException("高難易度競技は実行中ではありません");
        if (targetTick < elapsedTick || targetTick >= totalTicks) throw new IllegalArgumentException("指定時刻が範囲外です");
        long previous = elapsedTick;
        elapsedTick = targetTick;
        evaluateElimination(previous, elapsedTick);
        rankingDirty = true;
        var next = nextElimination();
        display.updateImmediately(elapsedTick, totalTicks, next == null ? null : next.tick(),
                next == null ? null : next.requiredCourse(), session);
    }

    public synchronized void forceFinish() throws PersistenceException {
        if (session == null) throw new IllegalStateException("高難易度競技は実行中ではありません");
        finish();
    }

    @EventHandler
    public void onReturnItem(PlayerInteractEvent event) {
        if (!returnItem.isReturnItem(event.getItem())) return;
        event.setCancelled(true);
        HighCompetitionSession current = session;
        Player player = event.getPlayer();
        if (current == null || !current.contains(player.getUniqueId()) || !current.active(player.getUniqueId())) return;
        if (current.record(player.getUniqueId()).allCoursesCleared()) {
            returnItem.remove(player);
            player.sendMessage(configuration.message(
                    "notifications.high.checkpoint-disabled",
                    "[BEAT] 完走済みのためCPは使用できません。"));
            return;
        }
        int course = current.currentCourse(player.getUniqueId());
        player.setVelocity(new Vector());
        teleport(player, maps.high().courses().get(course).start());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        HighCompetitionSession current = session;
        Player player = event.getPlayer();
        if (current == null) return;
        if (current.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> restore(player));
        } else if (admins.isAdmin(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (display != null && player.isOnline()) display.add(player);
            });
        }
    }

    public synchronized void shutdown() {
        if (task != null) task.cancel();
        task = null;
        if (display != null && session != null) display.clear(session);
        if (session != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (session.contains(player.getUniqueId())) returnItem.remove(player);
            }
        }
        display = null;
        session = null;
    }

    public void resetRegisteredPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (rosters.current().participant(player.getUniqueId()).isPresent()) {
                returnItem.remove(player);
                player.setFlying(false);
                player.setAllowFlight(false);
                player.setVelocity(new Vector());
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    private synchronized void tick() {
        if (session == null) return;
        long previousTick = elapsedTick++;
        scanRegions();
        announceUpcomingElimination();
        evaluateElimination(previousTick, elapsedTick);
        announceTimeLimit();
        if (rankingDirty) {
            display.updateRanking(session);
            rankingDirty = false;
        }
        if (elapsedTick >= totalTicks) {
            try {
                finish();
            } catch (PersistenceException exception) {
                plugin.getLogger().log(Level.SEVERE, "高難易度結果を保存できませんでした", exception);
                shutdown();
            }
            return;
        }
        var next = nextElimination();
        display.update(elapsedTick, totalTicks, next == null ? null : next.tick(),
                next == null ? null : next.requiredCourse(), session);
    }

    private void scanRegions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!session.contains(uuid) || !session.active(uuid)) continue;
            MapLocation location = mapLocation(player);
            Target target = targetAt(location);
            if (target == null) continue;
            try {
                if (target.goal()) handleGoal(player, target.course());
                else handleSpot(player, target.course(), target.spot());
            } catch (IllegalStateException ignored) {
                // A player may have been frozen earlier in this tick by another event.
            }
        }
    }

    private Target targetAt(MapLocation location) {
        Target best = null;
        int bestOrder = -1;
        for (int course = 1; course <= 5; course++) {
            HighCourseMap map = maps.high().courses().get(course);
            for (var entry : map.spots().entrySet()) {
                if (entry.getValue().isSteppedOnBy(location)) {
                    int order = course * 10_000 + entry.getKey();
                    if (order > bestOrder) { best = new Target(course, entry.getKey(), false); bestOrder = order; }
                }
            }
            if (map.goal().isSteppedOnBy(location)) {
                int order = course * 10_000 + map.spots().size() + 1;
                if (order > bestOrder) { best = new Target(course, 0, true); bestOrder = order; }
            }
        }
        return best;
    }

    private void handleSpot(Player player, int course, int spot) {
        var result = session.reachSpot(player.getUniqueId(), course, spot, elapsedTick);
        if (!result.score().changed()) return;
        rankingDirty = true;
        display.feedback(player, configuration.message(
                "notifications.high.spot",
                "Spot {spot} 到達 ｜ {points}pt (+{delta})",
                Map.of(
                        "spot", spot,
                        "points", "%03d".formatted(result.score().currentPoints()),
                        "delta", result.score().delta())),
                elapsedTick + configuration.styleInt("display-ticks.feedback", 20, 1, 1200));
        playConfigured(player, "sounds.high-spot", Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.5F);
        notifyRankChange(player, result);
    }

    private void handleGoal(Player player, int course) {
        var result = session.reachGoal(player.getUniqueId(), course, elapsedTick);
        if (!result.score().changed()) return;
        rankingDirty = true;
        if (result.allClear()) {
            returnItem.remove(player);
            display.feedback(player, configuration.message(
                    "notifications.high.all-clear",
                    "ALL CLEAR! ｜ {points}pt ｜ #{rank}",
                    Map.of(
                            "points", "%03d".formatted(result.score().currentPoints()),
                            "rank", "%02d".formatted(result.currentRank()))),
                    elapsedTick + configuration.styleInt("display-ticks.goal", 60, 1, 1200));
            Bukkit.broadcastMessage(configuration.message(
                    "notifications.high.all-clear-broadcast",
                    "[BEAT] {player} が全コースクリア！ {points}pt (#{rank})",
                    Map.of(
                            "player", session.competitor(player.getUniqueId()).tournamentName(),
                            "points", result.score().currentPoints(),
                            "rank", result.currentRank())));
            playConfigured(player, "sounds.high-goal", Sound.UI_TOAST_CHALLENGE_COMPLETE, 1F, 1F);
            player.sendMessage(configuration.message(
                    "notifications.high.checkpoint-disabled",
                    "[BEAT] 完走したためCPを使用できなくなりました。"));
        } else {
            display.feedback(player, configuration.message(
                    "notifications.high.course-clear",
                    "Course {course} Clear! ｜ {points}pt (+{delta})",
                    Map.of(
                            "course", course,
                            "points", "%03d".formatted(result.score().currentPoints()),
                            "delta", result.score().delta())),
                    elapsedTick + configuration.styleInt("display-ticks.feedback", 20, 1, 1200));
            playConfigured(player, "sounds.high-course", Sound.ENTITY_PLAYER_LEVELUP, 1F, 1.2F);
            player.sendMessage(configuration.message(
                    "notifications.high.checkpoint-updated",
                    "[BEAT] CPが{course}に更新されました。",
                    Map.of("course", session.currentCourse(player.getUniqueId()))));
            notifyRankChange(player, result);
        }
    }

    private void notifyRankChange(Player player, HighCompetitionSession.ScoreResult result) {
        if (result.rankChanged()) Bukkit.broadcastMessage(configuration.message(
                "notifications.high.rank-update",
                "[BEAT] {player} が更新: {points}pt (#{rank})",
                Map.of(
                        "player", session.competitor(player.getUniqueId()).tournamentName(),
                        "points", result.score().currentPoints(),
                        "rank", result.currentRank())));
    }

    private void announceUpcomingElimination() {
        for (int index = 0; index < eliminationTicks.size(); index++) {
            long remaining = eliminationTicks.get(index) - elapsedTick;
            if (remaining >= 20 && remaining <= 200 && remaining % 20 == 0) {
                int requiredCourse = index + 2;
                forAudience(player -> {
                    player.sendMessage(configuration.message(
                            "notifications.high.elimination-warning",
                            "[BEAT] コース{course}未到達者脱落まで... {seconds}",
                            Map.of("course", requiredCourse, "seconds", remaining / 20)));
                    playConfigured(player, "sounds.countdown", Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F);
                });
            }
        }
    }

    private void announceTimeLimit() {
        long remaining = totalTicks - elapsedTick;
        if (remaining > 0 && remaining <= 200 && remaining % 20 == 0) {
            forAudience(player -> {
                player.sendMessage(configuration.message(
                        "notifications.high.time-limit-countdown",
                        "[BEAT] 制限時間終了まで... {seconds}",
                        Map.of("seconds", remaining / 20)));
                playConfigured(player, "sounds.countdown", Sound.BLOCK_NOTE_BLOCK_HAT, 1F, 1F);
            });
        } else if (remaining == 0) {
            forAudience(player -> {
                player.sendMessage(configuration.message(
                        "notifications.high.time-limit-ended", "[BEAT] 制限時間終了！"));
                playConfigured(player, "sounds.time-limit-end", Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 0.5F);
            });
        }
    }

    private void playConfigured(Player player, String path, Sound fallback, float volume, float pitch) {
        player.playSound(
                player.getLocation(),
                configuration.sound(path, fallback),
                configuration.soundVolume(path, volume),
                configuration.soundPitch(path, pitch));
    }

    private void evaluateElimination(long previousTick, long currentTick) {
        for (int index = 0; index < eliminationTicks.size(); index++) {
            long boundary = eliminationTicks.get(index);
            if (previousTick >= boundary || currentTick < boundary || !processedEliminations.add(index)) continue;
            int requiredCourse = index + 2;
            Bukkit.broadcastMessage(configuration.message(
                    "notifications.high.elimination",
                    "[BEAT] コース{course}未到達者脱落！",
                    Map.of("course", requiredCourse)));
            forAudience(player -> playConfigured(
                    player, "sounds.elimination", Sound.BLOCK_NOTE_BLOCK_BASS, 1F, 0.7F));
            var eliminatedPlayers = session.eliminateBelowCourse(requiredCourse);
            rankingDirty = true;
            for (var eliminated : eliminatedPlayers) {
                Player player = Bukkit.getPlayer(eliminated.playerId());
                if (player != null) {
                    player.setGameMode(GameMode.SPECTATOR);
                    returnItem.remove(player);
                    player.sendMessage(configuration.message(
                            "notifications.high.eliminated",
                            "[BEAT] 脱落しました。最終記録: {points}pt (#{rank})",
                            Map.of("points", eliminated.points(), "rank", eliminated.rank())));
                }
            }
        }
    }

    private void finish() throws PersistenceException {
        session.finish();
        results.saveFinal(session);
        eventState.transitionTo(TournamentState.HIGH_FINISHED);
        display.updateRanking(session);
        forParticipants(player -> {
            player.setGameMode(GameMode.ADVENTURE);
            player.setVelocity(new Vector());
            returnItem.remove(player);
            teleport(player, maps.high().end());
        });
        display.clear(session);
        Bukkit.broadcastMessage(configuration.message(
                "notifications.high.finished",
                "[BEAT] 高難易度競技が終了し、結果を確定しました。"));
        if (task != null) task.cancel();
        task = null;
        display = null;
        session = null;
    }

    private void activateAtStart(Player player) {
        session.activate(player.getUniqueId());
        CompetitionPlayerState.normalize(player);
        teleport(player, maps.high().courses().get(1).start());
        returnItem.give(player);
        display.add(player);
        rankingDirty = true;
    }

    private void restore(Player player) {
        if (session.eliminated(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);
            rankingDirty = true;
            return;
        }
        CompetitionPlayerState.normalize(player);
        if (!session.active(player.getUniqueId())) {
            if (!eliminationTicks.isEmpty() && elapsedTick >= eliminationTicks.getFirst()) {
                player.setGameMode(GameMode.SPECTATOR);
                rankingDirty = true;
                return;
            }
            session.activate(player.getUniqueId());
            player.setGameMode(GameMode.ADVENTURE);
            teleport(player, maps.high().courses().get(1).start());
        }
        returnItem.give(player);
        display.add(player);
        rankingDirty = true;
    }

    private NextElimination nextElimination() {
        for (int index = 0; index < eliminationTicks.size(); index++) {
            if (eliminationTicks.get(index) > elapsedTick) return new NextElimination(eliminationTicks.get(index), index + 2);
        }
        return null;
    }

    private record NextElimination(long tick, int requiredCourse) { }

    private void forParticipants(java.util.function.Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) if (session.contains(player.getUniqueId())) action.accept(player);
    }

    private void forAudience(java.util.function.Consumer<Player> action) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (session.contains(player.getUniqueId()) || admins.isAdmin(player.getUniqueId())) action.accept(player);
        }
    }

    private static MapLocation mapLocation(Player player) {
        Location l = player.getLocation();
        return new MapLocation(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    private static void teleport(Player player, MapLocation target) {
        if (target == null) return;
        World world = Bukkit.getWorld(target.world());
        if (world != null) player.teleport(new Location(world, target.x(), target.y(), target.z(), target.yaw(), target.pitch()));
    }

    private record Target(int course, int spot, boolean goal) {}
}
