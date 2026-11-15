package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

/**
 * 平台侧上下文：把「玩家名 → PlayerRef」与「默认世界」解析交给各平台适配器。
 */
public interface GameServerContext {

    /**
     * 按玩家名解析在线玩家；玩家不在线时返回离线句柄（UUID 与 Bukkit OfflinePlayer 一致）。
     */
    PlayerRef resolvePlayer(String username);

    /** 高度查询与构建使用的默认世界（通常为主世界）。 */
    WorldRef defaultWorld();
}
