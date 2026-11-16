package com.structengine.paper.adapter;

import com.structengine.buildmodel.remote.GameServerContext;
import com.structengine.buildmodel.remote.OfflinePlayerRef;
import com.structengine.buildmodel.remote.RemotePlayerInfo;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Paper 平台上下文：在线玩家解析与主世界句柄。
 */
public final class PaperGameServerContext implements GameServerContext {

    @Override
    public PlayerRef resolvePlayer(String username) {
        Player player = Bukkit.getPlayerExact(username);
        if (player != null) {
            return new PaperPlayerRef(player);
        }
        return OfflinePlayerRef.of(username);
    }

    @Override
    public List<RemotePlayerInfo> onlinePlayers() {
        long now = System.currentTimeMillis();
        return Bukkit.getOnlinePlayers().stream().map(player -> {
            Location location = player.getLocation();
            return new RemotePlayerInfo(player.getUniqueId().toString(), player.getName(),
                    player.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    true, now);
        }).toList();
    }

    @Override
    public WorldRef defaultWorld() {
        if (Bukkit.getWorlds().isEmpty()) {
            throw new IllegalStateException("服务端尚未加载任何世界");
        }
        World world = Bukkit.getWorlds().get(0);
        return new PaperWorldRef(world);
    }
}
