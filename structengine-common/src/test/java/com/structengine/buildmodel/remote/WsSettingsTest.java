package com.structengine.buildmodel.remote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsSettingsTest {

    @Test
    void endpointAppendsServerId() {
        WsSettings settings = WsSettings.of("ws://platform:8080/ws/game", "survival", "secret");
        assertTrue(settings.enabled());
        assertEquals("ws://platform:8080/ws/game?serverId=survival", settings.endpoint().toString());
    }

    @Test
    void endpointKeepsExistingQuery() {
        WsSettings settings = WsSettings.of("ws://platform/ws/game?foo=bar", "srv", "");
        assertEquals("ws://platform/ws/game?foo=bar&serverId=srv", settings.endpoint().toString());
    }

    @Test
    void normalizesHttpSchemes() {
        assertEquals("ws://platform/ws/game?serverId=default",
                WsSettings.of("http://platform/ws/game", "", "").endpoint().toString());
        assertEquals("wss://platform/ws/game?serverId=default",
                WsSettings.of("https://platform/ws/game", null, null).endpoint().toString());
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class, () -> WsSettings.of("ftp://platform/game", "s", ""));
    }

    @Test
    void rejectsInvalidServerId() {
        assertThrows(IllegalArgumentException.class, () -> WsSettings.of("ws://x", "bad id", ""));
    }

    @Test
    void disabledWhenUrlBlank() {
        WsSettings settings = WsSettings.of("", "survival", "");
        assertFalse(settings.enabled());
        assertThrows(IllegalStateException.class, settings::endpoint);
    }

    @Test
    void toStringRedactsToken() {
        String text = WsSettings.of("ws://x", "s", "super-secret").toString();
        assertTrue(text.contains("redacted"));
        assertFalse(text.contains("super-secret"));
    }
}
