package dev.konqasasas.beat.ui;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Hides only the world entity and restores its Tab entry through ProtocolLib. */
final class ProtocolPlayerVisibility {
    private final Plugin plugin;
    private final ProtocolManager protocol=ProtocolLibrary.getProtocolManager();
    ProtocolPlayerVisibility(Plugin plugin){this.plugin=plugin;}

    void hideBodyKeepTab(Player viewer,Player target){
        viewer.hidePlayer(plugin,target);
        PacketContainer info=protocol.createPacket(PacketType.Play.Server.PLAYER_INFO);
        info.getPlayerInfoActions().write(0,EnumSet.of(
                EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
                EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
                EnumWrappers.PlayerInfoAction.UPDATE_LATENCY,
                EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME,
                EnumWrappers.PlayerInfoAction.UPDATE_LIST_ORDER,
                EnumWrappers.PlayerInfoAction.UPDATE_HAT));
        PlayerInfoData data=new PlayerInfoData(
                target.getUniqueId(),target.getPing(),true,
                EnumWrappers.NativeGameMode.fromBukkit(target.getGameMode()),
                WrappedGameProfile.fromPlayer(target),
                WrappedChatComponent.fromLegacyText(target.getPlayerListName()),
                true,target.getPlayerListOrder(),null);
        info.getPlayerInfoDataLists().write(1,List.of(data));
        protocol.sendServerPacket(viewer,info);
    }
    void show(Player viewer,Player target){viewer.showPlayer(plugin,target);}
}
