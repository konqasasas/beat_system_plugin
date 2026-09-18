package dev.konqasasas.beat.ui;

import dev.konqasasas.beat.BeatPlugin;
import dev.konqasasas.beat.application.AdminAuthorizer;
import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.roster.RosterService;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

public final class PlayerVisibilityService implements Listener {
    private final BeatPlugin plugin;private final RosterService rosters;private final AdminAuthorizer admins;
    private final ConfigurationFiles configuration;
    private final PlayerVisibilityPolicy policy=new PlayerVisibilityPolicy();private final ProtocolPlayerVisibility packets;
    private final NamespacedKey marker;private BukkitTask maintenance;
    public PlayerVisibilityService(BeatPlugin plugin,RosterService rosters,AdminAuthorizer admins,ConfigurationFiles configuration){this.plugin=plugin;this.rosters=rosters;this.admins=admins;this.configuration=configuration;packets=new ProtocolPlayerVisibility(plugin);marker=new NamespacedKey(plugin,"player-visibility-toggle");}
    public void start(){long interval=configuration.configInt("ui-update-ticks.player-visibility",20,1,1200);maintenance=Bukkit.getScheduler().runTaskTimer(plugin,()->Bukkit.getOnlinePlayers().forEach(player->{if(participant(player))ensureItem(player);if(adminOnly(player)){player.setPlayerListOrder(10_000);player.setPlayerListName(configuration.message("player-visibility.admin-tab","&c[ADMIN] &f{player}",Map.of("player",player.getName())));}}),1,interval);}
    @EventHandler public void use(PlayerInteractEvent event){if(!isItem(event.getItem()))return;event.setCancelled(true);Player viewer=event.getPlayer();if(!participant(viewer))return;boolean visible=policy.toggle(viewer.getUniqueId());apply(viewer);viewer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,net.md_5.bungee.api.chat.TextComponent.fromLegacy(visible?configuration.message("player-visibility.shown","&aプレイヤー表示: ON"):configuration.message("player-visibility.hidden","&cプレイヤー表示: OFF")));playConfigured(viewer,"sounds.player-visibility",Sound.BLOCK_NOTE_BLOCK_PLING,0.8F,visible?1.4F:0.8F);}
    @EventHandler public void drop(PlayerDropItemEvent event){if(isItem(event.getItemDrop().getItemStack()))event.setCancelled(true);}
    @EventHandler public void join(PlayerJoinEvent event){Bukkit.getScheduler().runTask(plugin,()->{Player joined=event.getPlayer();if(participant(joined)){ensureItem(joined);apply(joined);}for(Player viewer:Bukkit.getOnlinePlayers())if(participant(viewer)&&policy.shouldHide(viewer.getUniqueId(),joined.getUniqueId(),participant(joined),adminOnly(joined)))packets.hideBodyKeepTab(viewer,joined);});}
    public void shutdown(){if(maintenance!=null)maintenance.cancel();maintenance=null;for(Player viewer:Bukkit.getOnlinePlayers()){if(admins.isAdmin(viewer.getUniqueId())){viewer.setPlayerListOrder(0);viewer.setPlayerListName(viewer.getName());}for(Player target:Bukkit.getOnlinePlayers())if(!viewer.equals(target))packets.show(viewer,target);}policy.clear();}
    private void apply(Player viewer){for(Player target:Bukkit.getOnlinePlayers()){if(viewer.equals(target))continue;if(policy.shouldHide(viewer.getUniqueId(),target.getUniqueId(),participant(target),adminOnly(target)))packets.hideBodyKeepTab(viewer,target);else packets.show(viewer,target);}}
    private boolean participant(Player player){return rosters.current().participant(player.getUniqueId()).isPresent();}
    private boolean adminOnly(Player player){return admins.isAdmin(player.getUniqueId())&&!participant(player);}
    private void ensureItem(Player player){for(ItemStack item:player.getInventory().getContents())if(isItem(item))return;String path="player-visibility.item";Material material=Material.matchMaterial(plugin.getConfig().getString(path+".material","ENDER_EYE"));if(material==null||material.isAir())material=Material.ENDER_EYE;ItemStack item=new ItemStack(material);var meta=item.getItemMeta();meta.setDisplayName(color(plugin.getConfig().getString(path+".name","&bプレイヤー表示切替")));List<String> lore=plugin.getConfig().getStringList(path+".lore").stream().map(PlayerVisibilityService::color).toList();meta.setLore(lore);meta.getPersistentDataContainer().set(marker,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);int slot=Math.max(0,Math.min(8,plugin.getConfig().getInt(path+".slot",8)));ItemStack existing=player.getInventory().getItem(slot);if(existing==null||existing.getType().isAir())player.getInventory().setItem(slot,item);else player.getInventory().addItem(item);}
    private boolean isItem(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(marker,PersistentDataType.BYTE);}
    private void playConfigured(Player player,String path,Sound fallback,float volume,float pitch){player.playSound(player.getLocation(),configuration.sound(path,fallback),configuration.soundVolume(path,volume),configuration.soundPitch(path,pitch));}
    private static String color(String text){return ChatColor.translateAlternateColorCodes('&',text);}
}
