package com.structengine.buildmodel.remote;

/**
 * 上报给 Platform 的在线玩家快照。
 */
public record RemotePlayerInfo(
        String uuid,
        String name,
        String world,
        double x,
        double y,
        double z,
        boolean online,
        Long lastSeen
) {
}
