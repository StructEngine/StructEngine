package com.structengine.buildmodel.spi;

import java.util.UUID;

/**
 * 玩家句柄：屏蔽各平台 ServerPlayer / Player 类型差异。
 * <p>业务层只依赖 UUID 与适配器提供的能力，不感知具体平台实现。
 */
public interface PlayerRef {

    /** 玩家唯一标识（离线续建、历史记录均以此为准）。 */
    UUID id();
}
