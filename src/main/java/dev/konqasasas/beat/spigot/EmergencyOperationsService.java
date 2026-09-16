package dev.konqasasas.beat.spigot;

import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.application.HighResultService;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.RosterService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Shared implementation used by both the emergency command and inventory GUI. */
public final class EmergencyOperationsService {
    private final RosterService rosters;
    private final EventStateService states;
    private final MapConfigurationService maps;
    private final HighResultService highResults;
    private final HighPracticeController highPractice;
    private final HighCompetitionController highCompetition;
    private final TimeAttackController timeAttack;
    private final EnduranceController endurance;

    public EmergencyOperationsService(RosterService rosters, EventStateService states,
            MapConfigurationService maps, HighResultService highResults,
            HighPracticeController highPractice, HighCompetitionController highCompetition,
            TimeAttackController timeAttack, EnduranceController endurance) {
        this.rosters = rosters;
        this.states = states;
        this.maps = maps;
        this.highResults = highResults;
        this.highPractice = highPractice;
        this.highCompetition = highCompetition;
        this.timeAttack = timeAttack;
        this.endurance = endurance;
    }

    public void cancelPhase() throws PersistenceException {
        highPractice.shutdown();
        highCompetition.shutdown();
        timeAttack.shutdown();
        endurance.shutdown();
        states.emergencyResetCurrentPhase();
    }

    /** Stops live competition tasks without changing persistent tournament state. */
    public void shutdownForEventReset() {
        highPractice.shutdown();
        highCompetition.shutdown();
        timeAttack.shutdown();
        endurance.shutdown();
        highCompetition.resetRegisteredPlayers();
    }

    public int collectParticipants() {
        MapLocation destination = endLocation(states.current().tournamentState());
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (rosters.current().participant(player.getUniqueId()).isEmpty()) continue;
            player.setGameMode(GameMode.ADVENTURE);
            player.setVelocity(new Vector());
            if (destination != null) teleport(player, destination);
            count++;
        }
        return count;
    }

    public void forceEnd() throws PersistenceException {
        switch (states.current().tournamentState()) {
            case HIGH_RUNNING -> highCompetition.forceFinish();
            case TA_RUNNING -> timeAttack.forceFinish();
            case ENDURANCE_RUNNING -> endurance.forceFinish();
            default -> throw new IllegalStateException("強制終了できる競技が実行中ではありません");
        }
    }

    public TournamentState restartCurrent() throws PersistenceException {
        TournamentState current = states.current().tournamentState();
        switch (current) {
            case HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE, HIGH_PREPARE, HIGH_RUNNING -> {
                boolean repeatPractice = current == TournamentState.HIGH_PRACTICE_COUNTDOWN
                        || current == TournamentState.HIGH_PRACTICE;
                highPractice.shutdown();
                highCompetition.shutdown();
                highCompetition.resetRegisteredPlayers();
                highResults.clearHigh();
                states.resetHighForRestart(repeatPractice);
            }
            case TA_COUNTDOWN, TA_RUNNING -> {
                timeAttack.shutdown();
                states.emergencyResetCurrentPhase();
            }
            case ENDURANCE_COUNTDOWN, ENDURANCE_RUNNING -> {
                endurance.shutdown();
                states.emergencyResetCurrentPhase();
            }
            default -> throw new IllegalStateException("再試合に戻せる競技が進行中ではありません");
        }
        return states.current().tournamentState();
    }

    private MapLocation endLocation(TournamentState state) {
        return switch (state) {
            case HIGH_PRACTICE_COUNTDOWN, HIGH_PRACTICE, HIGH_PREPARE, HIGH_RUNNING, HIGH_FINISHED -> maps.high().end();
            case TA_READY, TA_COUNTDOWN, TA_RUNNING, TA_FINISHED -> maps.timeAttack().end();
            case ENDURANCE_READY, ENDURANCE_COUNTDOWN, ENDURANCE_RUNNING, ENDURANCE_FINISHED -> maps.endurance().end();
            default -> null;
        };
    }

    private static void teleport(Player player, MapLocation target) {
        World world = Bukkit.getWorld(target.world());
        if (world != null) player.teleport(new Location(
                world, target.x(), target.y(), target.z(), target.yaw(), target.pitch()));
    }
}
