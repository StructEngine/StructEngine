package com.structengine.neoforge.adapter;

import com.structengine.buildmodel.remote.GameServerContext;
import com.structengine.buildmodel.remote.OfflinePlayerRef;
import com.structengine.buildmodel.remote.RemotePlayerInfo;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * NeoForge 平台上下文：在线玩家解析与主世界句柄。
 */
public final class NeoForgeGameServerContext implements GameServerContext {

    private final MinecraftServer server;

    public NeoForgeGameServerContext(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public PlayerRef resolvePlayer(String username) {
        ServerPlayer player = server.getPlayerList().getPlayer(username);
        if (player != null) {
            return new NeoForgePlayerRef(player);
        }
        return OfflinePlayerRef.of(username);
    }

    @Override
    public List<RemotePlayerInfo> onlinePlayers() {
        long now = System.currentTimeMillis();
        return server.getPlayerList().getPlayers().stream().map(player ->
                new RemotePlayerInfo(player.getUUID().toString(), player.getName().getString(),
                        player.level().dimension().identifier().toString(),
                        player.getX(), player.getY(), player.getZ(), true, now)).toList();
    }

    @Override
    public WorldRef defaultWorld() {
        return new NeoForgeWorldRef(server.overworld());
    }
}
