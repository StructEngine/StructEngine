package com.structengine.buildmodel.remote;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 插件连接 Platform 的配置，全部来自环境变量 / 系统属性：
 *
 * <ul>
 *   <li>{@code STRUCTENGINE_WS_URL}：必填，形如 {@code ws://platform:8080/ws/game}；</li>
 *   <li>{@code STRUCTENGINE_SERVER_ID}：可选，默认 {@code default}，多服部署时必须唯一；</li>
 *   <li>{@code STRUCTENGINE_WS_TOKEN}：可选，对应 Platform 的 {@code STRUCTENGINE_GAME_TOKEN}；</li>
 *   <li>{@code STRUCTENGINE_REMOTE_ENABLED}：可选，默认 {@code true}，设为 false 可关闭网络层。</li>
 * </ul>
 */
public record WsSettings(String url, String serverId, String token) {

    private static final Pattern SERVER_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    public static WsSettings fromEnvironment() {
        String enabled = value("STRUCTENGINE_REMOTE_ENABLED", "true");
        String url = value("STRUCTENGINE_WS_URL", "").trim();
        String serverId = value("STRUCTENGINE_SERVER_ID", "default").trim();
        String token = value("STRUCTENGINE_WS_TOKEN", "").trim();

        if (!Boolean.parseBoolean(enabled) || url.isEmpty()) {
            return new WsSettings("", serverId.isEmpty() ? "default" : serverId, token);
        }
        if (!SERVER_ID.matcher(serverId).matches()) {
            throw new IllegalArgumentException("STRUCTENGINE_SERVER_ID 非法: " + serverId);
        }
        return new WsSettings(normalizeUrl(url), serverId, token);
    }

    public static WsSettings of(String url, String serverId, String token) {
        String normalizedServerId = (serverId == null || serverId.isBlank()) ? "default" : serverId.trim();
        if (!SERVER_ID.matcher(normalizedServerId).matches()) {
            throw new IllegalArgumentException("serverId 非法: " + normalizedServerId);
        }
        return new WsSettings((url == null || url.isBlank()) ? "" : normalizeUrl(url.trim()),
                normalizedServerId, token == null ? "" : token.trim());
    }

    public boolean enabled() {
        return url != null && !url.isBlank();
    }

    public URI endpoint() {
        if (!enabled()) {
            throw new IllegalStateException("未配置 STRUCTENGINE_WS_URL");
        }
        String base = url;
        String separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + "serverId=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return "WsSettings{url='" + url + "', serverId='" + serverId + "', token="
                + (token == null || token.isEmpty() ? "<empty>" : "<redacted>") + "}";
    }

    private static String normalizeUrl(String url) {
        String normalized = url;
        if (normalized.startsWith("http://")) {
            normalized = "ws://" + normalized.substring("http://".length());
        } else if (normalized.startsWith("https://")) {
            normalized = "wss://" + normalized.substring("https://".length());
        }
        if (!normalized.startsWith("ws://") && !normalized.startsWith("wss://")) {
            throw new IllegalArgumentException("STRUCTENGINE_WS_URL 必须是 ws:// 或 wss:// 地址: " + url);
        }
        return normalized;
    }

    private static String value(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null ? fallback : value;
    }
}
