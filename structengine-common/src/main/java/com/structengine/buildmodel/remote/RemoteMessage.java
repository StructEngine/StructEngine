package com.structengine.buildmodel.remote;

/**
 * 双向 WebSocket 统一消息体，字段与 Platform 的 GameMessage 对齐。
 * <p>空字段由 Gson 自动省略；反序列化时未知字段会被忽略，便于协议向前兼容。</p>
 */
public record RemoteMessage(
        RemoteAction action,
        String taskId,
        String playerName,
        Integer x,
        Integer y,
        Integer z,
        String modelUrl,
        String message
) {

    public static RemoteMessage queryY(String taskId, String playerName, int x, int z) {
        return new RemoteMessage(RemoteAction.QUERY_Y, taskId, playerName, x, null, z, null, null);
    }

    public static RemoteMessage responseY(String taskId, int y) {
        return new RemoteMessage(RemoteAction.RESPONSE_Y, taskId, null, null, y, null, null, null);
    }

    public static RemoteMessage build(String taskId, String playerName, int x, int y, int z, String modelUrl) {
        return new RemoteMessage(RemoteAction.BUILD, taskId, playerName, x, y, z, modelUrl, null);
    }

    public static RemoteMessage ping() {
        return new RemoteMessage(RemoteAction.PING, null, null, null, null, null, null, null);
    }

    public static RemoteMessage pong() {
        return new RemoteMessage(RemoteAction.PONG, null, null, null, null, null, null, null);
    }

    public static RemoteMessage error(String taskId, String message) {
        return new RemoteMessage(RemoteAction.ERROR, taskId, null, null, null, null, null, message);
    }
}
