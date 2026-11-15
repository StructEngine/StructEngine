package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.spi.PlayerRef;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 离线玩家句柄：UUID 使用与 Bukkit 相同的 OfflinePlayer 算法，保证历史记录可延续。
 */
public record OfflinePlayerRef(UUID id, String name) implements PlayerRef {

    public static OfflinePlayerRef of(String username) {
        String name = username == null ? "" : username;
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return new OfflinePlayerRef(uuid, name);
    }
}
