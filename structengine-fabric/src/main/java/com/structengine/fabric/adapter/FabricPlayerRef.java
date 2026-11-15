package com.structengine.fabric.adapter;

import com.structengine.buildmodel.spi.PlayerRef;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Fabric 玩家句柄：包装 {@link ServerPlayerEntity}。
 */
public record FabricPlayerRef(ServerPlayerEntity handle) implements PlayerRef {

    @Override
    public UUID id() {
        return handle.getUuid();
    }
}
