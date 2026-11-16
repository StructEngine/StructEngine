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
 *   <li>{@code STRUCTENGINE_SERVER_NAME}：可选，展示名，默认等于 serverId；</li>
 *   <li>{@code STRUCTENGINE_WS_TOKEN}：可选，对应 Platform 的 {@code STRUCTENGINE_GAME_TOKEN}；</li>
 *   <li>{@code STRUCTENGINE_BLUEMAP_URL}：该服的 BlueMap 定制地图页地址；</li>
 *   <li>{@code STRUCTENGINE_BLUEMAP_MAP_ID}：BlueMap 地图 ID，默认 world；</li>
 *   <li>{@code STRUCTENGINE_REMOTE_ENABLED}：可选，默认 true。</li>
 * </ul>
 */
public record WsSettings(
        String url,
        String serverId,
        String token,
        String serverName,
        String blueMapUrl,
        String mapId
) {

    private static final Pattern SERVER_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** 兼容旧测试/调用。 */
    public WsSettings(String url, String serverId, String token) {
        this(url, serverId, token, serverId, "", "world");
    }

    public static WsSettings fromEnvironment() {
        String enabled = value("STRUCTENGINE_REMOTE_ENABLED", "true");
        String url = value("STRUCTENGINE_WS_URL", "").trim();
        String serverId = value("STRUCTENGINE_SERVER_ID", "default").trim();
        String serverName = value("STRUCTENGINE_SERVER_NAME", "").trim();
        String token = value("STRUCTENGINE_WS_TOKEN", "").trim();
        String blueMapUrl = value("STRUCTENGINE_BLUEMAP_URL", "").trim();
        String mapId = value("STRUCTENGINE_BLUEMAP_MAP_ID", "world").trim();

        String normalizedServerId = serverId.isEmpty() ? "default" : serverId;
        if (!Boolean.parseBoolean(enabled) || url.isEmpty()) {
            return new WsSettings("", normalizedServerId, token,
                    serverName.isEmpty() ? normalizedServerId : serverName, blueMapUrl,
                    mapId.isEmpty() ? "world" : mapId);
        }
        if (!SERVER_ID.matcher(normalizedServerId).matches()) {
            throw new IllegalArgumentException("STRUCTENGINE_SERVER_ID 非法: " + normalizedServerId);
        }
        return new WsSettings(normalizeUrl(url), normalizedServerId, token,
                serverName.isEmpty() ? normalizedServerId : serverName, blueMapUrl,
                mapId.isEmpty() ? "world" : mapId);
    }

    public static WsSettings of(String url, String serverId, String token) {
        String normalizedServerId = (serverId == null || serverId.isBlank()) ? "default" : serverId.trim();
        if (!SERVER_ID.matcher(normalizedServerId).matches()) {
            throw new IllegalArgumentException("serverId 非法: " + normalizedServerId);
        }
        return new WsSettings((url == null || url.isBlank()) ? "" : normalizeUrl(url.trim()),
                normalizedServerId, token == null ? "" : token.trim(),
                normalizedServerId, "", "world");
    }

    public static WsSettings of(String url, String serverId, String token,
                                String serverName, String blueMapUrl, String mapId) {
        WsSettings base = of(url, serverId, token);
        return new WsSettings(base.url(), base.serverId(), base.token(),
                serverName == null || serverName.isBlank() ? base.serverId() : serverName.trim(),
                blueMapUrl == null ? "" : blueMapUrl.trim(),
                mapId == null || mapId.isBlank() ? "world" : mapId.trim());
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
        return "WsSettings{url='" + url + "', serverId='" + serverId + "', serverName='" + serverName
                + "', blueMapUrl='" + blueMapUrl + "', mapId='" + mapId + "', token="
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
