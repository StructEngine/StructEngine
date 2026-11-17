package com.structengine.buildmodel.remote;

import java.net.URI;

/**
 * 插件连接 Platform 的配置，全部来自环境变量 / 系统属性：
 *
 * <ul>
 *   <li>{@code STRUCTENGINE_PLATFORM_URL}：必填，形如 {@code ws://platform:8080/ws/game}；</li>
 *   <li>{@code STRUCTENGINE_SERVER_SECRET}：必填，Platform 服务器详情中生成的一次性 secret；</li>
 *   <li>{@code STRUCTENGINE_SERVER_NAME}：可选，展示名；</li>
 *   <li>{@code STRUCTENGINE_BLUEMAP_URL}：该服的 BlueMap 定制地图页地址；</li>
 *   <li>{@code STRUCTENGINE_BLUEMAP_MAP_ID}：BlueMap 地图 ID，默认 world；</li>
 *   <li>{@code STRUCTENGINE_REMOTE_ENABLED}：可选，默认 true。</li>
 * </ul>
 *
 * <p>兼容旧变量 {@code STRUCTENGINE_WS_URL} 作为 Platform URL 的 fallback；
 * 旧 {@code STRUCTENGINE_SERVER_ID} / {@code STRUCTENGINE_WS_TOKEN} 已废弃。</p>
 */
public record WsSettings(
        String url,
        String secret,
        String serverName,
        String blueMapUrl,
        String mapId
) {

    /** 兼容测试/旧调用：只给 URL 与 secret。 */
    public WsSettings(String url, String secret) {
        this(url, secret, "Minecraft Server", "", "world");
    }

    public static WsSettings fromEnvironment() {
        String enabled = value("STRUCTENGINE_REMOTE_ENABLED", "true");
        String url = value("STRUCTENGINE_PLATFORM_URL",
                value("STRUCTENGINE_WS_URL", "")).trim();
        String secret = value("STRUCTENGINE_SERVER_SECRET", "").trim();
        String serverName = value("STRUCTENGINE_SERVER_NAME", "").trim();
        String blueMapUrl = value("STRUCTENGINE_BLUEMAP_URL", "").trim();
        String mapId = value("STRUCTENGINE_BLUEMAP_MAP_ID", "world").trim();

        if (!Boolean.parseBoolean(enabled) || url.isEmpty()) {
            return new WsSettings("", secret,
                    serverName.isEmpty() ? "Minecraft Server" : serverName,
                    blueMapUrl, mapId.isEmpty() ? "world" : mapId);
        }
        if (secret.isEmpty()) {
            throw new IllegalArgumentException(
                    "已配置 Platform URL，但缺少 STRUCTENGINE_SERVER_SECRET");
        }
        return new WsSettings(normalizeUrl(url), secret,
                serverName.isEmpty() ? "Minecraft Server" : serverName,
                blueMapUrl, mapId.isEmpty() ? "world" : mapId);
    }

    public static WsSettings of(String url, String secret) {
        return new WsSettings((url == null || url.isBlank()) ? "" : normalizeUrl(url.trim()),
                secret == null ? "" : secret.trim(), "Minecraft Server", "", "world");
    }

    public static WsSettings of(String url, String secret, String serverName,
                                String blueMapUrl, String mapId) {
        WsSettings base = of(url, secret);
        return new WsSettings(base.url(), base.secret(),
                serverName == null || serverName.isBlank() ? base.serverName() : serverName.trim(),
                blueMapUrl == null ? "" : blueMapUrl.trim(),
                mapId == null || mapId.isBlank() ? "world" : mapId.trim());
    }

    public boolean enabled() {
        return url != null && !url.isBlank();
    }

    public URI endpoint() {
        if (!enabled()) {
            throw new IllegalStateException("未配置 STRUCTENGINE_PLATFORM_URL");
        }
        return URI.create(url);
    }

    @Override
    public String toString() {
        return "WsSettings{url='" + url + "', serverName='" + serverName
                + "', blueMapUrl='" + blueMapUrl + "', mapId='" + mapId + "', secret="
                + (secret == null || secret.isEmpty() ? "<empty>" : "<redacted>") + "}";
    }

    private static String normalizeUrl(String url) {
        String normalized = url;
        if (normalized.startsWith("http://")) {
            normalized = "ws://" + normalized.substring("http://".length());
        } else if (normalized.startsWith("https://")) {
            normalized = "wss://" + normalized.substring("https://".length());
        }
        if (!normalized.startsWith("ws://") && !normalized.startsWith("wss://")) {
            throw new IllegalArgumentException("Platform URL 必须是 ws:// 或 wss:// 地址: " + url);
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
