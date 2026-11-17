package com.structengine.buildmodel.remote;

import java.util.List;

/**
 * 双向 WebSocket 统一消息体（协议 v3），字段与 Platform 的 GameMessage 对齐。
 * <p>v3 使用每服 secret 作为身份；所有上行消息都带 secret。空字段由 Gson 省略。</p>
 */
public record RemoteMessage(
        RemoteAction action,
        Integer protocolVersion,
        String serverId,
        String serverName,
        String taskId,
        String requestId,
        String playerUuid,
        String playerName,
        String world,
        String blueMapUrl,
        String mapId,
        Integer x,
        Integer y,
        Integer z,
        Integer centerX,
        Integer centerZ,
        Integer rotation,
        String modelUrl,
        String modelName,
        String historyId,
        String state,
        Integer placed,
        Integer total,
        Boolean full,
        List<RemotePlayerInfo> players,
        List<RemoteHistoryEntry> history,
        Long timestamp,
        String message,
        String secret
) {

    public static final int PROTOCOL_VERSION = 3;

    public static RemoteMessage hello(String serverName, String blueMapUrl, String mapId,
                                     String secret) {
        return new RemoteMessage(RemoteAction.HELLO, PROTOCOL_VERSION, null, serverName,
                null, null, null, null, null, blueMapUrl, mapId,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, System.currentTimeMillis(), null, secret);
    }

    public static RemoteMessage playerList(List<RemotePlayerInfo> players, boolean full,
                                           String secret) {
        return new RemoteMessage(RemoteAction.PLAYER_LIST, PROTOCOL_VERSION, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, full, players, null, System.currentTimeMillis(), null, secret);
    }

    public static RemoteMessage queryY(String taskId, String playerName, int x, int z) {
        return new RemoteMessage(RemoteAction.QUERY_Y, PROTOCOL_VERSION, null, null,
                taskId, null, null, playerName, null, null, null,
                x, null, z, null, null, null, null, null, null,
                null, null, null, null, null, null, System.currentTimeMillis(), null, null);
    }

    public static RemoteMessage responseY(String taskId, int y, String secret) {
        return new RemoteMessage(RemoteAction.RESPONSE_Y, PROTOCOL_VERSION, null, null,
                taskId, null, null, null, null, null, null,
                null, y, null, null, null, null, null, null, null,
                null, null, null, null, null, null, System.currentTimeMillis(), null, secret);
    }

    public static RemoteMessage buildStatus(String taskId, String playerUuid, String state,
                                            Integer placed, Integer total, String message,
                                            String secret) {
        return new RemoteMessage(RemoteAction.BUILD_STATUS, PROTOCOL_VERSION, null, null,
                taskId, null, playerUuid, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                state, placed, total, null, null, null, System.currentTimeMillis(), message, secret);
    }

    public static RemoteMessage historyResponse(String requestId, List<RemoteHistoryEntry> history,
                                                String secret) {
        return new RemoteMessage(RemoteAction.HISTORY_RESPONSE, PROTOCOL_VERSION, null, null,
                null, requestId, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, history, System.currentTimeMillis(), null, secret);
    }

    public static RemoteMessage ping(String secret) {
        return new RemoteMessage(RemoteAction.PING, PROTOCOL_VERSION, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, System.currentTimeMillis(), null, secret);
    }

    public static RemoteMessage pong() {
        return new RemoteMessage(RemoteAction.PONG, PROTOCOL_VERSION, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, System.currentTimeMillis(), null, null);
    }

    public static RemoteMessage error(String taskId, String message, String secret) {
        return new RemoteMessage(RemoteAction.ERROR, PROTOCOL_VERSION, null, null,
                taskId, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, System.currentTimeMillis(), message, secret);
    }
}
