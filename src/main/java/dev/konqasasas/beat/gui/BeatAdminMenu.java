package dev.konqasasas.beat.gui;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.application.EventStateService;
import dev.konqasasas.beat.application.OverallService;
import dev.konqasasas.beat.application.ResultAnnouncementService;
import dev.konqasasas.beat.application.WhitelistService;
import dev.konqasasas.beat.command.CompetitionCommand;
import dev.konqasasas.beat.command.CompetitionSettingsCommand;
import dev.konqasasas.beat.configuration.CompetitionSettings;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.configuration.ConfigurationLoadException;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.domain.state.TournamentState;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.map.validation.MapValidationService;
import dev.konqasasas.beat.map.validation.ValidationReport;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import dev.konqasasas.beat.roster.RegisteredIdentity;
import dev.konqasasas.beat.roster.RosterService;
import dev.konqasasas.beat.spigot.EmergencyOperationsService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class BeatAdminMenu implements Listener {
    private final BeatPlugin plugin;private final AdminAuthorizer admins;private final RosterService rosters;
    private final EventStateService states;private final MapConfigurationService maps;private final MapValidationService validation;
    private final CompetitionCommand competitions;private final EmergencyOperationsService emergency;
    private final OverallService overall;private final WhitelistService whitelists;private final NamespacedKey actionKey;
    private final ResultAnnouncementService announcements;
    private final ConfigurationFiles configuration;
    private final CompetitionSettingsService settings;
    private final CompetitionSettingsCommand settingsCommand;
    private final Map<UUID,CompetitionSettings> settingsDrafts=new java.util.HashMap<>();

    public BeatAdminMenu(BeatPlugin plugin,AdminAuthorizer admins,RosterService rosters,EventStateService states,
            MapConfigurationService maps,MapValidationService validation,CompetitionCommand competitions,
            EmergencyOperationsService emergency,OverallService overall,WhitelistService whitelists,
            ResultAnnouncementService announcements,ConfigurationFiles configuration,
            CompetitionSettingsService settings,CompetitionSettingsCommand settingsCommand){
        this.plugin=plugin;this.admins=admins;this.rosters=rosters;this.states=states;this.maps=maps;
        this.validation=validation;this.competitions=competitions;this.emergency=emergency;
        this.overall=overall;this.whitelists=whitelists;this.announcements=announcements;this.configuration=configuration;this.settings=settings;this.settingsCommand=settingsCommand;actionKey=new NamespacedKey(plugin,"admin-menu-action");
    }

    public void open(Player player){Inventory inventory=create(BeatMenuHolder.Screen.MAIN,m("gui.titles.main","BEAT 運営メニュー"),54);
        put(inventory,10,Material.DIAMOND_BOOTS,m("gui.main.high","高難易度"),"start_high",ml("gui.main-lore.high",List.of("状態: {state}"),Map.of("state",states.current().tournamentState())));
        put(inventory,12,Material.CLOCK,m("gui.main.time-attack","タイムアタック"),"start_ta",ml("gui.main-lore.time-attack",List.of("開始前に自動Validation")));
        put(inventory,14,Material.IRON_BOOTS,m("gui.main.endurance","耐久"),"start_endurance",ml("gui.main-lore.endurance",List.of("開始前に自動Validation")));
        put(inventory,16,Material.NETHER_STAR,m("gui.main.overall","総合"),"overall",ml("gui.main-lore.overall",List.of("計算・確認・確定")));
        put(inventory,28,Material.PLAYER_HEAD,m("gui.main.players","Players"),"players",ml("gui.main-lore.players",List.of("登録参加者一覧・個人状態")));
        put(inventory,30,Material.WRITABLE_BOOK,m("gui.main.results","結果"),"results",ml("gui.main-lore.results",List.of("競技結果・再計算・確定")));
        put(inventory,32,Material.BLAZE_ROD,m("gui.main.setup","Setup"),"setup",ml("gui.main-lore.setup",List.of("全マップValidation")));
        put(inventory,34,Material.IRON_BARS,m("gui.main.whitelist","Whitelist"),"whitelist",ml("gui.main-lore.whitelist",List.of("同期状態・モード切替")));
        put(inventory,40,Material.REPEATER,m("gui.main.settings","競技設定"),"settings",List.of("制限時間・脱落時刻・TA生存人数"));
        put(inventory,49,Material.REDSTONE_BLOCK,m("gui.main.emergency","緊急操作"),"emergency",ml("gui.main-lore.emergency",List.of("通常操作とは分離されています")));player.openInventory(inventory);}

    @EventHandler public void onClick(InventoryClickEvent event){if(!(event.getInventory().getHolder() instanceof BeatMenuHolder))return;event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)||!admins.isAdmin(player.getUniqueId())){event.getWhoClicked().closeInventory();return;}
        String action=action(event.getCurrentItem());if(action==null)return;
        try{
            if(action.startsWith("players_page:")){openPlayers(player,number(action,1));return;}
            if(action.startsWith("player:")){openPlayer(player,UUID.fromString(part(action,1)));return;}
            if(action.startsWith("player_flag:")){playerFlag(player,part(action,1),UUID.fromString(part(action,2)));return;}
            if(action.startsWith("result_open:")){openResult(player,part(action,1),0);return;}
            if(action.startsWith("result_page:")){openResult(player,part(action,1),number(action,2));return;}
            if(action.startsWith("result_command:")){String kind=part(action,1);runAndReturn(player,"beat result "+kind+" "+part(action,2),()->returnResult(player,kind));return;}
            if(action.startsWith("result_announce_confirm:")){openAnnouncementConfirmation(player,part(action,1));return;}
            if(action.startsWith("result_announce:")){announce(player,part(action,1));return;}
            if(action.startsWith("overall_page:")){openOverall(player,number(action,1));return;}
            if(action.startsWith("overall_command:")){runAndReturn(player,"beat overall "+part(action,1),()->returnOverall(player));return;}
            if(action.startsWith("whitelist_mode:")){runAndReturn(player,"beat whitelist "+part(action,1),()->openWhitelist(player));return;}
            if(action.startsWith("validation:")){openValidation(player,part(action,1));return;}
            if(action.startsWith("settings_adjust:")){adjustSetting(player,part(action,1),event.isLeftClick(),event.isShiftClick());return;}
            switch(action){
                case"back"->open(player);case"players"->openPlayers(player,0);case"results"->openResults(player);
                case"setup"->openSetup(player);case"whitelist"->openWhitelist(player);case"overall"->openOverall(player,0);case"settings"->openSettings(player);
                case"settings_preset_production"->{settingsDrafts.put(player.getUniqueId(),CompetitionSettings.production());openSettings(player);}
                case"settings_preset_test"->{settingsDrafts.put(player.getUniqueId(),CompetitionSettings.testPreset());openSettings(player);}
                case"settings_apply"->applySettings(player);case"settings_discard"->{settingsDrafts.remove(player.getUniqueId());open(player);}
                case"start_high"->openStart(player,"high");case"start_ta"->openStart(player,"ta");case"start_endurance"->openStart(player,"endurance");
                case"confirm_start_high"->start(player,"high");case"confirm_start_ta"->start(player,"ta");case"confirm_start_endurance"->start(player,"endurance");
                case"emergency"->openEmergency(player);
                case"cancel_phase"->openDanger(player,BeatMenuHolder.Screen.CONFIRM_CANCEL,m("gui.confirm-titles.cancel","フェーズを中止しますか？"),"confirm_cancel");
                case"collect"->openDanger(player,BeatMenuHolder.Screen.CONFIRM_COLLECT,m("gui.confirm-titles.collect","参加者を回収しますか？"),"confirm_collect");
                case"force_end"->openDanger(player,BeatMenuHolder.Screen.CONFIRM_FORCE_END,m("gui.confirm-titles.force-end","現在の競技を強制終了しますか？"),"confirm_force_end");
                case"restart_current"->openDanger(player,BeatMenuHolder.Screen.CONFIRM_RESTART,m("gui.confirm-titles.restart","現在の競技を再試合準備へ戻しますか？"),"confirm_restart");
                case"confirm_cancel"->cancelPhase(player);case"confirm_collect"->collect(player);case"confirm_force_end"->forceEnd(player);case"confirm_restart"->restartCurrent(player);
                default->{ }
            }
        }catch(PersistenceException|ConfigurationLoadException|IllegalArgumentException|IllegalStateException exception){player.sendMessage(guiError(exception));}
    }

    private void openSettings(Player player){
        CompetitionSettings draft=settingsDrafts.computeIfAbsent(player.getUniqueId(),ignored->settings.current());
        Inventory inventory=create(BeatMenuHolder.Screen.SETTINGS,"BEAT 競技設定",54);
        int[] slots={9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};
        for(int i=0;i<CompetitionSettingsCommand.KEYS.size();i++){
            String key=CompetitionSettingsCommand.KEYS.get(i);long value=draft.value(key);boolean count=key.startsWith("ta-survivors-");
            put(inventory,slots[i],count?Material.PLAYER_HEAD:Material.CLOCK,settingLabel(key),"settings_adjust:"+key,
                    List.of("現在: "+(count?value:CompetitionSettingsCommand.clock(value)),"左: +"+(count?"1人":"30秒")+" / 右: -"+(count?"1人":"30秒"),"Shift: "+(count?"5人":"5分")+"単位"));
        }
        put(inventory,37,Material.IRON_BLOCK,"本番プリセット","settings_preset_production",List.of("本番時間を下書きへ読み込み"));
        put(inventory,39,Material.REDSTONE_TORCH,"テストプリセット","settings_preset_test",List.of("制限時間系を本番の半分にする","開始10秒・TA生存人数は維持"));
        put(inventory,43,Material.EMERALD_BLOCK,"変更を適用","settings_apply",List.of("competition-settings.ymlへ保存"));
        put(inventory,45,Material.BARRIER,"変更を破棄","settings_discard",List.of());
        put(inventory,49,Material.ARROW,m("gui.common.back","戻る"),"back",List.of("下書きはこの画面を離れても保持"));
        player.openInventory(inventory);
    }

    private void adjustSetting(Player player,String key,boolean increase,boolean shift){
        CompetitionSettings draft=settingsDrafts.computeIfAbsent(player.getUniqueId(),ignored->settings.current());
        boolean count=key.startsWith("ta-survivors-");long step=count?(shift?5:1):(shift?6_000:600);long next=draft.value(key)+(increase?step:-step);
        settingsDrafts.put(player.getUniqueId(),draft.with(key,next));openSettings(player);
    }

    private void applySettings(Player player)throws ConfigurationLoadException{
        settingsCommand.ensureEditable();CompetitionSettings draft=settingsDrafts.getOrDefault(player.getUniqueId(),settings.current());
        settings.save(draft);settingsDrafts.remove(player.getUniqueId());player.sendMessage("[BEAT] 競技設定を保存しました。");open(player);
    }

    private static String settingLabel(String key){return switch(key){
        case"start-countdown"->"開始カウントダウン";case"high-practice"->"高難易度 練習";case"high-prepare"->"高難易度 準備";case"high-total"->"高難易度 制限時間";
        case"ta-total"->"TA 制限時間";case"endurance-total"->"耐久 制限時間";
        default->key.replace("high-elimination-","高難易度 脱落 ").replace("ta-elimination-","TA 脱落 ").replace("endurance-elimination-","耐久 脱落 ").replace("ta-survivors-","TA 生存人数 ");};}

    private void openPlayers(Player player,int requestedPage){List<RegisteredIdentity> rows=rosters.current().participants().values().stream().sorted(Comparator.comparing(RegisteredIdentity::mcid,String.CASE_INSENSITIVE_ORDER)).toList();int page=MenuPagination.clamp(requestedPage,rows.size());Inventory inventory=create(BeatMenuHolder.Screen.PLAYERS,configuration.message("gui.titles.players","BEAT Players {page}/{pages}",Map.of("page",page+1,"pages",MenuPagination.pages(rows.size()))),54);for(int i=MenuPagination.from(page);i<MenuPagination.to(page,rows.size());i++){var identity=rows.get(i);boolean online=Bukkit.getPlayer(identity.uuid())!=null;put(inventory,i%MenuPagination.PAGE_SIZE,online?Material.LIME_DYE:Material.GRAY_DYE,identity.mcid(),"player:"+identity.uuid(),ml("gui.players.row-lore",List.of("{uuid}","{online}","大会名: {tournamentName}"),Map.of("uuid",identity.uuid(),"online",online?m("gui.common.online","Online"):m("gui.common.offline","Offline"),"tournamentName",states.tournamentName(identity.uuid()).orElse(m("gui.common.not-recorded","未記録")))));}navigation(inventory,page,rows.size(),"players_page:","back");player.openInventory(inventory);}

    private void openPlayer(Player player,UUID uuid)throws PersistenceException{RegisteredIdentity identity=rosters.current().participant(uuid).orElseThrow(()->new IllegalArgumentException("参加者が見つかりません"));PlayerResultSnapshot result=overall.current().players().stream().filter(row->row.uuid().equals(uuid)).findFirst().orElse(null);Inventory inventory=create(BeatMenuHolder.Screen.PLAYER_DETAIL,configuration.message("gui.titles.player","BEAT Player: {player}",Map.of("player",identity.mcid())),36);Map<String,Object> values=new java.util.HashMap<>();values.put("uuid",uuid);values.put("online",Bukkit.getPlayer(uuid)!=null);values.put("tournamentName",states.tournamentName(uuid).orElse(m("gui.common.not-recorded","未記録")));values.put("high",result==null?"--":rank(result.high()==null?null:result.high().rank()));values.put("ta",result==null?"--":rank(result.timeAttack()==null?null:result.timeAttack().rank()));values.put("endurance",result==null?"--":rank(result.endurance()==null?null:result.endurance().rank()));values.put("excluded",result!=null&&result.overallExcluded());values.put("disqualified",result!=null&&result.disqualified());List<String> lore=ml("gui.player.detail-lore",List.of("UUID: {uuid}","Online: {online}","大会名: {tournamentName}","High: {high}","TA: {ta}","Endurance: {endurance}","総合除外: {excluded}","失格: {disqualified}"),values);put(inventory,13,Material.PLAYER_HEAD,identity.mcid(),"disabled",lore);put(inventory,20,Material.YELLOW_CONCRETE,m("gui.player.exclude","総合対象外にする"),"player_flag:overall-exclude:"+uuid,List.of());put(inventory,21,Material.LIME_CONCRETE,m("gui.player.include","総合対象へ戻す"),"player_flag:overall-include:"+uuid,List.of());put(inventory,23,Material.RED_CONCRETE,m("gui.player.disqualify","失格にする"),"player_flag:disqualify:"+uuid,List.of());put(inventory,24,Material.LIGHT_BLUE_CONCRETE,m("gui.player.undisqualify","失格を解除"),"player_flag:undisqualify:"+uuid,List.of());put(inventory,31,Material.ARROW,m("gui.player.back","Playersへ戻る"),"players_page:0",List.of());player.openInventory(inventory);}

    private void playerFlag(Player player,String operation,UUID uuid){RegisteredIdentity identity=rosters.current().participant(uuid).orElseThrow(()->new IllegalArgumentException("参加者が見つかりません"));runAndReturn(player,"beat player "+operation+" "+identity.mcid(),()->{try{openPlayer(player,uuid);}catch(PersistenceException e){player.sendMessage(guiError(e));}});}

    private void openResults(Player player)throws PersistenceException{var snapshot=overall.current();Inventory inventory=create(BeatMenuHolder.Screen.RESULTS,m("gui.titles.results","BEAT 競技結果"),27);put(inventory,10,Material.DIAMOND_BOOTS,m("gui.results.high","高難易度"),"result_open:high",List.of(resultState(snapshot.highConfirmed())));put(inventory,13,Material.CLOCK,m("gui.results.time-attack","TA"),"result_open:ta",List.of(resultState(snapshot.timeAttackConfirmed())));put(inventory,16,Material.IRON_BOOTS,m("gui.results.endurance","耐久"),"result_open:endurance",List.of(resultState(snapshot.enduranceConfirmed())));put(inventory,22,Material.ARROW,m("gui.common.back","戻る"),"back",List.of());player.openInventory(inventory);}

    private void openResult(Player player,String kind,int requestedPage)throws PersistenceException{var snapshot=overall.current();Predicate<PlayerResultSnapshot> has=switch(kind){case"high"->p->p.high()!=null;case"ta"->p->p.timeAttack()!=null;case"endurance"->p->p.endurance()!=null;default->throw new IllegalArgumentException("競技が不正です");};List<PlayerResultSnapshot> rows=snapshot.players().stream().filter(has).sorted(Comparator.comparingInt(p->resultRank(p,kind))).toList();int page=MenuPagination.clamp(requestedPage,rows.size());BeatMenuHolder.Screen screen=switch(kind){case"high"->BeatMenuHolder.Screen.RESULT_HIGH;case"ta"->BeatMenuHolder.Screen.RESULT_TA;default->BeatMenuHolder.Screen.RESULT_ENDURANCE;};Inventory inventory=create(screen,configuration.message("gui.titles.result","BEAT {competition} Result {page}/{pages}",Map.of("competition",kind.toUpperCase(Locale.ROOT),"page",page+1,"pages",MenuPagination.pages(rows.size()))),54);for(int i=MenuPagination.from(page);i<MenuPagination.to(page,rows.size());i++){var row=rows.get(i);put(inventory,i%MenuPagination.PAGE_SIZE,row.disqualified()?Material.BARRIER:Material.PAPER,configuration.message("gui.result.row-name","#{rank} {player}",Map.of("rank",rank(resultRankValue(row,kind)),"player",row.tournamentName())),"player:"+row.uuid(),resultLore(row,kind));}navigation(inventory,page,rows.size(),"result_page:"+kind+":","results");put(inventory,46,confirmed(snapshot,kind)?Material.NOTE_BLOCK:Material.GRAY_DYE,m("gui.result.announce","結果を発表"),confirmed(snapshot,kind)?"result_announce_confirm:"+kind:"disabled",ml("gui.result.announce-lore",List.of("確定済みの全順位を全員へ発表")));put(inventory,47,Material.COMPASS,m("gui.result.recalculate","順位を再計算"),"result_command:"+kind+":recalculate",ml("gui.result.recalculate-lore",List.of("元記録から再計算")));put(inventory,51,Material.EMERALD,m("gui.result.confirm","結果を確定"),"result_command:"+kind+":confirm",ml("gui.result.confirm-lore",List.of("確定後は通常編集不可")));player.openInventory(inventory);}

    private void openOverall(Player player,int requestedPage)throws PersistenceException{var snapshot=overall.current();List<PlayerResultSnapshot> rows=snapshot.players().stream().filter(p->p.overall()!=null).sorted(Comparator.comparingInt(p->p.overall().rank())).toList();int page=MenuPagination.clamp(requestedPage,rows.size());Inventory inventory=create(BeatMenuHolder.Screen.OVERALL,configuration.message("gui.titles.overall","BEAT Overall {page}/{pages}",Map.of("page",page+1,"pages",MenuPagination.pages(rows.size()))),54);for(int i=MenuPagination.from(page);i<MenuPagination.to(page,rows.size());i++){var row=rows.get(i);var value=row.overall();put(inventory,i%MenuPagination.PAGE_SIZE,Material.NETHER_STAR,configuration.message("gui.overall.row-name","#{rank} {player}",Map.of("rank",rank(value.rank()),"player",row.tournamentName())),"player:"+row.uuid(),ml("gui.overall.row-lore",List.of("持ち点: x{score}","[{high} * {ta} * {endurance}]","{state}"),Map.of("score",value.scoreProduct(),"high",value.highRank(),"ta",value.taRank(),"endurance",value.enduranceRank(),"state",resultState(value.confirmed()))));}navigation(inventory,page,rows.size(),"overall_page:","back");put(inventory,46,snapshot.overallConfirmed()?Material.NOTE_BLOCK:Material.GRAY_DYE,m("gui.result.announce","結果を発表"),snapshot.overallConfirmed()?"result_announce_confirm:overall":"disabled",ml("gui.result.announce-lore",List.of("確定済みの全順位を全員へ発表")));put(inventory,47,Material.COMPASS,m("gui.overall.calculate","総合を計算"),"overall_command:calculate",List.of());put(inventory,51,Material.EMERALD,m("gui.overall.confirm","総合を確定"),"overall_command:confirm",List.of(snapshot.overallConfirmed()?m("gui.overall.confirmed","確定済み"):m("gui.overall.needs-confirmation","要確認")));player.openInventory(inventory);}

    private void openWhitelist(Player player){var status=whitelists.status();Inventory inventory=create(BeatMenuHolder.Screen.WHITELIST,m("gui.titles.whitelist","BEAT Whitelist"),27);put(inventory,4,status.synchronizedExactly()?Material.LIME_CONCRETE:Material.YELLOW_CONCRETE,configuration.message("gui.whitelist.current","現在: {mode}",Map.of("mode",status.mode())),"disabled",ml("gui.whitelist.status-lore",List.of("設定対象: {configured}","Whitelist登録: {actual}","{sync}"),Map.of("configured",status.configuredPlayers(),"actual",status.whitelistedPlayers(),"sync",status.synchronizedExactly()?m("gui.whitelist.synchronized","完全同期済み"):m("gui.whitelist.difference","差分あり"))));put(inventory,11,Material.IRON_BARS,m("gui.whitelist.admins","運営のみ"),"whitelist_mode:admins",ml("gui.whitelist.admins-lore",List.of("admins.jsonへ完全同期")));put(inventory,15,Material.IRON_DOOR,m("gui.whitelist.all","運営＋参加者"),"whitelist_mode:all",ml("gui.whitelist.all-lore",List.of("admins + participantsへ完全同期")));put(inventory,22,Material.ARROW,m("gui.common.back","戻る"),"back",List.of());player.openInventory(inventory);}

    private void openSetup(Player player){Inventory inventory=create(BeatMenuHolder.Screen.SETUP,m("gui.titles.setup","BEAT Setup Validation"),27);validationButton(inventory,10,"high",validation.validateHigh(maps.high()));validationButton(inventory,13,"ta",validation.validateTimeAttack(maps.timeAttack()));validationButton(inventory,16,"endurance",validation.validateEndurance(maps.endurance()));put(inventory,22,Material.ARROW,m("gui.common.back","戻る"),"back",List.of());player.openInventory(inventory);}
    private void validationButton(Inventory inventory,int slot,String kind,ValidationReport report){put(inventory,slot,report.errorCount()>0?Material.RED_CONCRETE:report.warningCount()>0?Material.YELLOW_CONCRETE:Material.LIME_CONCRETE,kind.toUpperCase(Locale.ROOT),"validation:"+kind,ml("gui.validation.summary-lore",List.of("ERROR: {errors}","WARNING: {warnings}","クリックで詳細"),Map.of("errors",report.errorCount(),"warnings",report.warningCount())));}
    private void openValidation(Player player,String kind){ValidationReport report=switch(kind){case"high"->validation.validateHigh(maps.high());case"ta"->validation.validateTimeAttack(maps.timeAttack());case"endurance"->validation.validateEndurance(maps.endurance());default->throw new IllegalArgumentException("競技が不正です");};Inventory inventory=create(BeatMenuHolder.Screen.VALIDATION,configuration.message("gui.titles.validation","Validation: {competition}",Map.of("competition",kind)),54);int slot=0;for(var issue:report.issues().stream().limit(MenuPagination.PAGE_SIZE).toList())put(inventory,slot++,issue.severity().name().equals("ERROR")?Material.RED_DYE:Material.YELLOW_DYE,issue.severity().name(),"disabled",List.of(issue.message()));if(report.issues().isEmpty())put(inventory,22,Material.LIME_CONCRETE,m("gui.validation.pass","PASS"),"disabled",ml("gui.validation.no-issues",List.of("問題はありません")));put(inventory,49,Material.ARROW,m("gui.validation.back","Setupへ戻る"),"setup",List.of());player.openInventory(inventory);}

    private void openStart(Player player,String competition){ValidationReport report=switch(competition){case"high"->validation.validateHigh(maps.high());case"ta"->validation.validateTimeAttack(maps.timeAttack());default->validation.validateEndurance(maps.endurance());};Inventory inventory=create(switch(competition){case"high"->BeatMenuHolder.Screen.START_HIGH;case"ta"->BeatMenuHolder.Screen.START_TA;default->BeatMenuHolder.Screen.START_ENDURANCE;},configuration.message("gui.titles.start","{competition} 開始確認",Map.of("competition",competition.toUpperCase(Locale.ROOT))),27);List<String> lore=new ArrayList<>(ml("gui.start.summary-lore",List.of("参加者: {participants}","Online: {online}","ERROR: {errors} / WARNING: {warnings}"),Map.of("participants",rosters.current().participants().size(),"online",rosters.current().participantUuids().stream().filter(id->Bukkit.getPlayer(id)!=null).count(),"errors",report.errorCount(),"warnings",report.warningCount())));report.issues().stream().limit(8).forEach(issue->lore.add(configuration.message("gui.start.issue","{severity}: {message}",Map.of("severity",issue.severity(),"message",issue.message()))));if(report.errorCount()>0)put(inventory,13,Material.BARRIER,m("gui.start.blocked","開始できません"),"disabled",lore);else put(inventory,13,report.warningCount()>0?Material.YELLOW_CONCRETE:Material.LIME_CONCRETE,report.warningCount()>0?m("gui.start.with-warning","警告を確認して開始"):m("gui.start.execute","開始する"),"confirm_start_"+competition,lore);put(inventory,22,Material.ARROW,m("gui.common.back","戻る"),"back",List.of());player.openInventory(inventory);}
    private void start(Player player,String competition){player.closeInventory();competitions.execute(player,new String[]{competition,"start"});}

    private void openEmergency(Player player){Inventory inventory=create(BeatMenuHolder.Screen.EMERGENCY,m("gui.titles.emergency","BEAT 緊急操作"),36);put(inventory,10,Material.BARRIER,m("gui.emergency.cancel","現在フェーズを中止"),"cancel_phase",ml("gui.emergency.cancel-lore",List.of("進行中データを保存せず安全状態へ","二段階確認")));put(inventory,12,Material.ENDER_PEARL,m("gui.emergency.collect","参加者を全回収"),"collect",ml("gui.emergency.collect-lore",List.of("現在競技の終了地点へ移動","二段階確認")));put(inventory,14,Material.REDSTONE_TORCH,m("gui.emergency.force-end","現在競技を強制終了"),"force_end",ml("gui.emergency.force-end-lore",List.of("現時点の結果を確定","競技中のみ実行可能","二段階確認")));put(inventory,16,Material.TNT,m("gui.emergency.restart","現在競技を再試合"),"restart_current",ml("gui.emergency.restart-lore",List.of("現在記録を破棄して準備状態へ","二段階確認")));put(inventory,31,Material.ARROW,m("gui.common.back","戻る"),"back",List.of());player.openInventory(inventory);}
    private void openDanger(Player player,BeatMenuHolder.Screen screen,String title,String action){Inventory inventory=create(screen,title,27);put(inventory,11,Material.RED_CONCRETE,m("gui.common.execute","実行する"),action,List.of(m("gui.emergency.irreversible","この操作は取り消せない場合があります")));put(inventory,15,Material.LIME_CONCRETE,m("gui.common.back","戻る"),"emergency",List.of());player.openInventory(inventory);}
    private void openAnnouncementConfirmation(Player player,String kind)throws PersistenceException{var snapshot=overall.current();if(kind.equals("overall")){if(!snapshot.overallConfirmed())throw new IllegalStateException("総合結果は未確定です");}else if(!confirmed(snapshot,kind))throw new IllegalStateException("競技結果は未確定です");Inventory inventory=create(BeatMenuHolder.Screen.CONFIRM_ANNOUNCE,m("gui.confirm-titles.announce","結果を全員に発表しますか？"),27);put(inventory,11,Material.NOTE_BLOCK,m("gui.result.announce-execute","発表する"),"result_announce:"+kind,ml("gui.result.announce-confirm-lore",List.of("全オンラインプレイヤーのChatへ送信")));put(inventory,15,Material.LIME_CONCRETE,m("gui.common.back","戻る"),kind.equals("overall")?"overall_page:0":"result_page:"+kind+":0",List.of());player.openInventory(inventory);}
    private void announce(Player player,String kind)throws PersistenceException{if(kind.equals("overall"))announcements.announceOverall();else announcements.announceCompetition(kind);player.closeInventory();}
    private void cancelPhase(Player player)throws PersistenceException{emergency.cancelPhase();player.closeInventory();player.sendMessage(m("gui.emergency.cancelled","[BEAT] 現在フェーズを安全状態へ中止しました。"));}
    private void collect(Player player){int count=emergency.collectParticipants();player.closeInventory();player.sendMessage(configuration.message("gui.emergency.collected","[BEAT] オンライン参加者 {count} 人を回収しました。",java.util.Map.of("count",count)));}
    private void forceEnd(Player player)throws PersistenceException{emergency.forceEnd();player.closeInventory();player.sendMessage(m("gui.emergency.forced","[BEAT] 現在の競技を強制終了し、結果を確定しました。"));}
    private void restartCurrent(Player player)throws PersistenceException{TournamentState target=emergency.restartCurrent();player.closeInventory();player.sendMessage(configuration.message("gui.emergency.restarted","[BEAT] 現在の競技を再試合準備へ戻しました: {state}",java.util.Map.of("state",target)));}

    private void runAndReturn(Player player,String command,Runnable returnView){player.closeInventory();player.performCommand(command);Bukkit.getScheduler().runTask(plugin,returnView);}
    private void returnResult(Player player,String kind){try{openResult(player,kind,0);}catch(PersistenceException exception){player.sendMessage(guiError(exception));}}
    private void returnOverall(Player player){try{openOverall(player,0);}catch(PersistenceException exception){player.sendMessage(guiError(exception));}}
    private void navigation(Inventory inventory,int page,int total,String pageAction,String backAction){if(page>0)put(inventory,45,Material.ARROW,m("gui.common.previous-page","前のページ"),pageAction+(page-1),List.of());put(inventory,49,Material.OAK_DOOR,m("gui.common.back","戻る"),backAction,List.of());if(MenuPagination.to(page,total)<total)put(inventory,53,Material.ARROW,m("gui.common.next-page","次のページ"),pageAction+(page+1),List.of());}
    private Inventory create(BeatMenuHolder.Screen screen,String title,int size){BeatMenuHolder holder=new BeatMenuHolder(screen);Inventory inventory=Bukkit.createInventory(holder,size,title);holder.inventory(inventory);return inventory;}
    private void put(Inventory inventory,int slot,Material material,String name,String action,List<String> lore){String key=material.name().toLowerCase(Locale.ROOT).replace('_','-');ItemStack item=new ItemStack(configuration.material("gui.items."+key+".material",material));var meta=item.getItemMeta();meta.setDisplayName("§f"+name);meta.setLore(lore.stream().map(line->"§7"+line).toList());meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);item.setItemMeta(meta);inventory.setItem(slot,item);}
    private String action(ItemStack item){return item==null||!item.hasItemMeta()?null:item.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);}
    private static String part(String action,int index){String[] parts=action.split(":",-1);if(index>=parts.length)throw new IllegalArgumentException("GUI actionが不正です");return parts[index];}
    private static int number(String action,int index){return Integer.parseInt(part(action,index));}
    private static String rank(Integer rank){return rank==null?"--":"%02d".formatted(rank);}
    private static int resultRank(PlayerResultSnapshot player,String kind){Integer rank=switch(kind){case"high"->player.high().rank();case"ta"->player.timeAttack().rank();default->player.endurance().rank();};return rank==null?Integer.MAX_VALUE:rank;}
    private static Integer resultRankValue(PlayerResultSnapshot player,String kind){return switch(kind){case"high"->player.high().rank();case"ta"->player.timeAttack().rank();default->player.endurance().rank();};}
    private static boolean confirmed(dev.konqasasas.beat.persistence.snapshot.ResultsSnapshot snapshot,String kind){return switch(kind){case"high"->snapshot.highConfirmed();case"ta"->snapshot.timeAttackConfirmed();case"endurance"->snapshot.enduranceConfirmed();default->false;};}
    private List<String> resultLore(PlayerResultSnapshot player,String kind){return switch(kind){case"high"->ml("gui.result.high-lore",List.of("Point: {points}","到達tick: {tick}","{flags}"),Map.of("points",player.high().points(),"tick",nullable(player.high().finalPointTick()),"flags",flags(player)));case"ta"->ml("gui.result.time-attack-lore",List.of("PB: {pb}","記録tick: {tick}","{flags}"),Map.of("pb",player.timeAttack().pbTicks()==null?"--.--":time(player.timeAttack().pbTicks()),"tick",nullable(player.timeAttack().pbRecordedTick()),"flags",flags(player)));default->ml("gui.result.endurance-lore",List.of("Progress: {progress}","到達tick: {tick}","Zone2: {zone2} / Zone3: {zone3}","{flags}"),Map.of("progress","%03d".formatted(player.endurance().maxProgress()),"tick",nullable(player.endurance().progressReachedTick()),"zone2",player.endurance().zone2Reached(),"zone3",player.endurance().zone3Reached(),"flags",flags(player)));};}
    private String flags(PlayerResultSnapshot player){return player.disqualified()?m("gui.result.disqualified","失格"):player.overallExcluded()?m("gui.result.excluded","総合対象外"):m("gui.result.valid","有効");}
    private static String nullable(Object value){return value==null?"--":value.toString();}
    private static String time(long ticks){return "%02d.%02d".formatted(ticks/20,(ticks%20)*5);}
    private String m(String path,String fallback){return configuration.message(path,fallback);}
    private List<String> ml(String path,List<String> fallback){return configuration.messages(path,fallback);}
    private List<String> ml(String path,List<String> fallback,Map<String,?> values){return configuration.messages(path,fallback,values);}
    private String resultState(boolean confirmed){return confirmed?m("gui.common.confirmed","確定"):m("gui.common.unconfirmed","未確定");}
    private String guiError(Exception exception){return configuration.message("gui.error","[BEAT] ERROR: {message}",java.util.Map.of("message",String.valueOf(exception.getMessage())));}
}
