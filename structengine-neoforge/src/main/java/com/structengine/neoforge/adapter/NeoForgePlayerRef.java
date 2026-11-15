package com.structengine.neoforge.adapter;

import com.structengine.buildmodel.spi.PlayerRef;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * NeoForge 玩家句柄：包装 {@link ServerPlayer}。
 */
public record NeoForgePlayerRef(ServerPlayer handle) implements PlayerRef {

    @Override
    public UUID id() {
        return handle.getUUID();
    }
}
