package com.structengine.fabric.adapter;

import com.structengine.buildmodel.remote.GameServerContext;
import com.structengine.buildmodel.remote.OfflinePlayerRef;
import com.structengine.buildmodel.remote.RemotePlayerInfo;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * Fabric 平台上下文：在线玩家解析与主世界句柄。
 */
public final class FabricGameServerContext implements GameServerContext {

    private final MinecraftServer server;

    public FabricGameServerContext(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerRef resolvePlayer(String username) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(username);
        if (player != null) {
            return new FabricPlayerRef(player);
        }
        return OfflinePlayerRef.of(username);
    }

    @Override
    public List<RemotePlayerInfo> onlinePlayers() {
        long now = System.currentTimeMillis();
        return server.getPlayerManager().getPlayerList().stream().map(player ->
                new RemotePlayerInfo(player.getUuidAsString(), player.getName().getString(),
                        player.getEntityWorld().getRegistryKey().getValue().toString(),
                        player.getX(), player.getY(), player.getZ(), true, now)).toList();
    }

    @Override
    public WorldRef defaultWorld() {
        return new FabricWorldRef(server.getOverworld());
    }
}
