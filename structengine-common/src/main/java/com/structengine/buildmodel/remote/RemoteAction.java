package com.structengine.buildmodel.remote;

/**
 * 与 Platform 约定的 WebSocket 消息类型（JSON 中以枚举名传输）。
 */
public enum RemoteAction {
    QUERY_Y,
    RESPONSE_Y,
    BUILD,
    CONNECTED,
    PING,
    PONG,
    ERROR
}
