package com.structengine.buildmodel.remote;

/**
 * 与 Platform 约定的 WebSocket 消息类型（协议 v2）。
 */
public enum RemoteAction {
    HELLO,
    PLAYER_LIST,
    QUERY_Y,
    RESPONSE_Y,
    BUILD,
    BUILD_STATUS,
    HISTORY_REQUEST,
    HISTORY_RESPONSE,
    ROLLBACK,
    REDO,
    CONNECTED,
    PING,
    PONG,
    ERROR
}
