package com.structengine.buildmodel.remote;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * RemoteMessage 的 Gson 编解码器（Gson 由 Minecraft 服务端运行时提供）。
 */
public final class RemoteMessageCodec {

    private final Gson gson = new Gson();

    public String encode(RemoteMessage message) {
        return gson.toJson(message);
    }

    public RemoteMessage decode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("消息体为空");
        }
        try {
            RemoteMessage message = gson.fromJson(payload, RemoteMessage.class);
            if (message == null || message.action() == null) {
                throw new IllegalArgumentException("消息缺少 action");
            }
            return message;
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("消息解析失败: " + e.getMessage(), e);
        }
    }
}
