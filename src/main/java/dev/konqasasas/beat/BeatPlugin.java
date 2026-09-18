package dev.konqasasas.beat;

import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.application.EventResetService;
import dev.konqasasas.beat.application.HighResultService;
import dev.konqasasas.beat.application.TimeAttackResultService;
import dev.konqasasas.beat.application.EnduranceResultService;
import dev.konqasasas.beat.application.OverallService;
import dev.konqasasas.beat.application.ResultAuditLog;
import dev.konqasasas.beat.application.ResultEditingService;
import dev.konqasasas.beat.application.ResultAnnouncementService;
import dev.konqasasas.beat.application.ParticipantService;
import dev.konqasasas.beat.application.WhitelistService;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.command.BeatCommand;
import dev.konqasasas.beat.command.CompetitionCommand;
import dev.konqasasas.beat.command.SetupCommand;
import dev.konqasasas.beat.command.ResultCommand;
import dev.konqasasas.beat.command.OverallCommand;
import dev.konqasasas.beat.gui.BeatAdminMenu;
import dev.konqasasas.beat.command.EmergencyCommand;
import dev.konqasasas.beat.command.EventCommand;
import dev.konqasasas.beat.command.DebugCommand;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.configuration.ConfigurationLoadException;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.debug.DebugService;
import dev.konqasasas.beat.spigot.EmergencyOperationsService;
import dev.konqasasas.beat.spigot.CompetitionSafetyListener;
import dev.konqasasas.beat.spigot.GameModeMonitor;
import dev.konqasasas.beat.ui.PlayerVisibilityService;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.map.validation.MapValidationService;
import dev.konqasasas.beat.persistence.PersistenceBootstrap;
import dev.konqasasas.beat.persistence.PersistenceContext;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.roster.GsonIdentityFileLoader;
import dev.konqasasas.beat.roster.RosterException;
import dev.konqasasas.beat.roster.RosterService;
import dev.konqasasas.beat.spigot.ParticipantJoinListener;
import dev.konqasasas.beat.spigot.SetupWandListener;
import dev.konqasasas.beat.spigot.HighPracticeController;
import dev.konqasasas.beat.spigot.HighCompetitionController;
import dev.konqasasas.beat.spigot.TimeAttackController;
import dev.konqasasas.beat.spigot.EnduranceController;
import dev.konqasasas.beat.spigot.EnduranceMarkerService;
import dev.konqasasas.beat.spigot.ParticipantProtectionListener;
import dev.konqasasas.beat.spigot.SpigotWhitelistGateway;
import dev.konqasasas.beat.spigot.LiveCompetitionClockService;
import dev.konqasasas.beat.setup.MapSetupService;
import dev.konqasasas.beat.setup.RegionVisualizer;
import dev.konqasasas.beat.setup.SelectionService;
import dev.konqasasas.beat.setup.SetupWand;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BeatPlugin extends JavaPlugin {
    private PersistenceContext persistence;
    private MapConfigurationService maps;
    private HighPracticeController highPractice;
    private HighCompetitionController highCompetition;
    private TimeAttackController timeAttack;
    private EnduranceController endurance;
    private EnduranceMarkerService enduranceMarkers;
    private PlayerVisibilityService playerVisibility;
    private ConfigurationFiles configurationFiles;
    private CompetitionSettingsService competitionSettings;

    @Override
    public void onEnable() {
        if (!getServer().getOnlineMode()) {
            getLogger().severe("online-mode=false はサポートされません。BEATを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("styles.yml", false);
        saveResource("participants.json", false);
        saveResource("admins.json", false);
        try {
            configurationFiles = ConfigurationFiles.load(this);
            competitionSettings = CompetitionSettingsService.load(this);
            persistence = new PersistenceBootstrap(getDataFolder().toPath()).initialize();
            maps = MapConfigurationService.open(getDataFolder().toPath().resolve("maps"));
            RosterService rosters = new RosterService(
                    new GsonIdentityFileLoader(),
                    getDataFolder().toPath().resolve("participants.json"),
                    getDataFolder().toPath().resolve("admins.json"));
            EventStateService eventState = new EventStateService(
                    persistence.eventStates(), persistence.eventState());
            TournamentState recoveredFrom = persistence.eventState().tournamentState();
            TournamentState recoveredTo = eventState.recoverAfterRestart();
            if (recoveredFrom != recoveredTo) {
                getLogger().warning("大会状態を再起動ポリシーに従って "
                        + recoveredFrom + " から " + recoveredTo + " へ戻しました。");
            }
            ParticipantService participants = new ParticipantService(rosters, eventState);
            AdminAuthorizer admins = new AdminAuthorizer(rosters);
            playerVisibility = new PlayerVisibilityService(this,rosters,admins,configurationFiles);
            WhitelistService whitelists = new WhitelistService(
                    rosters, eventState, new SpigotWhitelistGateway(getServer()));

            SelectionService selections = new SelectionService();
            SetupWand setupWand = new SetupWand(this);
            enduranceMarkers = new EnduranceMarkerService(this, admins);
            MapValidationService mapValidation =
                    new MapValidationService(world -> getServer().getWorld(world) != null);
            SetupCommand setupCommand = new SetupCommand(
                    maps,
                    new MapSetupService(maps),
                    selections,
                    setupWand,
                    new RegionVisualizer(this, configurationFiles, enduranceMarkers),
                    mapValidation,
                    getServer().getOnlineMode(),
                    configurationFiles);
            HighResultService highResults = new HighResultService(persistence.results());
            highCompetition = new HighCompetitionController(
                    this, rosters, eventState, maps, highResults, mapValidation, configurationFiles, competitionSettings);
            highPractice = new HighPracticeController(
                    this, rosters, eventState, maps, mapValidation, highCompetition, configurationFiles, competitionSettings);
            TimeAttackResultService taResults = new TimeAttackResultService(persistence.results());
            timeAttack = new TimeAttackController(
                    this, rosters, eventState, maps, mapValidation, taResults, configurationFiles, competitionSettings);
            EnduranceResultService enduranceResults=new EnduranceResultService(persistence.results());
            endurance=new EnduranceController(
                    this,rosters,eventState,maps,mapValidation,enduranceResults,configurationFiles,
                    competitionSettings,enduranceMarkers);
            OverallService overallService=new OverallService(persistence.results(),eventState);
            ResultAuditLog resultAudit=new ResultAuditLog(getDataFolder().toPath().resolve("logs/result-edits.log"));
            ResultEditingService resultEditor=new ResultEditingService(persistence.results(),maps,resultAudit);
            CompetitionCommand competitionCommand=new CompetitionCommand(
                    highPractice, highCompetition, timeAttack,endurance, eventState, highResults,
                    configurationFiles);
            EmergencyOperationsService emergencyOperations=new EmergencyOperationsService(
                    rosters,eventState,maps,highResults,highPractice,highCompetition,timeAttack,endurance);
            DebugService debugService=new DebugService(()->persistence.results().load()
                    .map(result->!result.players().isEmpty()||result.highConfirmed()
                            ||result.timeAttackConfirmed()||result.enduranceConfirmed()||result.overallConfirmed())
                    .orElse(false),maps,getConfig().getInt("high-difficulty.spot-points",50),
                    getConfig().getInt("high-difficulty.goal-points",100));
            var clockService = new LiveCompetitionClockService(highPractice, highCompetition, timeAttack, endurance);
            var settingsCommand = new dev.konqasasas.beat.command.CompetitionSettingsCommand(competitionSettings, eventState);
            BeatAdminMenu adminMenu=new BeatAdminMenu(this,admins,rosters,eventState,maps,mapValidation,
                    competitionCommand,emergencyOperations,overallService,whitelists,
                    new ResultAnnouncementService(overallService,configurationFiles),configurationFiles,
                    competitionSettings,settingsCommand);
            BeatCommand beatCommand = new BeatCommand(
                    rosters, eventState, whitelists, admins, setupCommand,
                    competitionCommand,
                    new ResultCommand(highResults, taResults,enduranceResults,resultEditor,configurationFiles),
                    new OverallCommand(overallService,configurationFiles),
                    new EventCommand(
                            new EventResetService(
                                    eventState,
                                    persistence.results(),
                                    persistence.backups(),
                                    getDataFolder().toPath().resolve("data/event-state.json"),
                                    getDataFolder().toPath().resolve("data/results.json")),
                            emergencyOperations,
                            debugService,
                            resultEditor,
                            configurationFiles),
                    new EmergencyCommand(emergencyOperations,configurationFiles),
                    new DebugCommand(debugService,highPractice,configurationFiles,clockService),settingsCommand,
                    overallService,adminMenu,configurationFiles,competitionSettings,
                    getLogger());
            PluginCommand command = Objects.requireNonNull(
                    getCommand("beat"), "beat command is missing from plugin.yml");
            command.setExecutor(beatCommand);
            command.setTabCompleter(beatCommand);

            ParticipantJoinListener joins = new ParticipantJoinListener(this, participants, admins);
            getServer().getPluginManager().registerEvents(joins, this);
            getServer().getPluginManager().registerEvents(
                    new SetupWandListener(setupWand, selections, admins, configurationFiles), this);
            getServer().getPluginManager().registerEvents(highPractice, this);
            getServer().getPluginManager().registerEvents(highCompetition, this);
            getServer().getPluginManager().registerEvents(timeAttack, this);
            getServer().getPluginManager().registerEvents(endurance,this);
            getServer().getPluginManager().registerEvents(enduranceMarkers,this);
            getServer().getPluginManager().registerEvents(adminMenu,this);
            getServer().getPluginManager().registerEvents(playerVisibility,this);
            getServer().getPluginManager().registerEvents(new CompetitionSafetyListener(eventState),this);
            getServer().getPluginManager().registerEvents(
                    new ParticipantProtectionListener(rosters, admins), this);
            getServer().getPluginManager().registerEvents(
                    new GameModeMonitor(this, rosters, admins, eventState, configurationFiles), this);
            playerVisibility.start();
            for (var onlinePlayer : getServer().getOnlinePlayers()) {
                joins.process(onlinePlayer);
            }
        } catch (PersistenceException | RosterException | ConfigurationLoadException exception) {
            getLogger().log(
                    Level.SEVERE,
                    "BEAT data could not be loaded safely. The plugin will be disabled without overwriting it.",
                    exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info(configurationFiles.message("plugin.enabled", "BEATを有効化しました。"));
    }

    @Override
    public void onDisable() {
        if (playerVisibility != null) playerVisibility.shutdown();
        if (highPractice != null) highPractice.shutdown();
        if (highCompetition != null) highCompetition.shutdown();
        if (timeAttack != null) timeAttack.shutdown();
        if(endurance!=null)endurance.shutdown();
        if(enduranceMarkers!=null)enduranceMarkers.shutdown();
        getLogger().info(configurationFiles == null
                ? "BEATを無効化しました。"
                : configurationFiles.message("plugin.disabled", "BEATを無効化しました。"));
    }
}
