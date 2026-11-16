package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;

/**
 * 平台侧上下文：把「玩家名 → PlayerRef」与「默认世界」解析交给各平台适配器。
 */
public interface GameServerContext {

    /**
     * 按玩家名解析在线玩家；玩家不在线时返回离线句柄（UUID 与 Bukkit OfflinePlayer 一致）。
     */
    PlayerRef resolvePlayer(String username);

    /**
     * 使用 Platform 指定的 UUID 解析玩家：在线时使用平台在线句柄，离线时构造稳定 UUID 句柄。
     */
    default PlayerRef resolvePlayer(String username, String uuid) {
        PlayerRef online = resolvePlayer(username);
        if (!(online instanceof OfflinePlayerRef)) {
            return online;
        }
        try {
            return OfflinePlayerRef.of(uuid == null ? null : java.util.UUID.fromString(uuid), username);
        } catch (IllegalArgumentException e) {
            return OfflinePlayerRef.of(username);
        }
    }

    /** 当前在线玩家快照（由 Platform 通过 WS 定期拉取）。 */
    List<RemotePlayerInfo> onlinePlayers();

    /** 高度查询与构建使用的默认世界（通常为主世界）。 */
    WorldRef defaultWorld();
}
