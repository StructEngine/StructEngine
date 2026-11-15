package com.structengine.paper.adapter;

import com.structengine.buildmodel.spi.PlayerRef;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Paper 玩家句柄：包装 {@link Player}。
 */
public record PaperPlayerRef(Player handle) implements PlayerRef {

    @Override
    public UUID id() {
        return handle.getUniqueId();
    }
}
