package com.structengine.buildmodel.remote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsSettingsTest {

    @Test
    void endpointUsesConfiguredUrl() {
        WsSettings settings = WsSettings.of("ws://platform:8080/ws/game", "secret");
        assertTrue(settings.enabled());
        assertEquals("ws://platform:8080/ws/game", settings.endpoint().toString());
        assertEquals("secret", settings.secret());
    }

    @Test
    void endpointKeepsExistingQuery() {
        WsSettings settings = WsSettings.of("ws://platform/ws/game?foo=bar", "secret");
        assertEquals("ws://platform/ws/game?foo=bar", settings.endpoint().toString());
    }

    @Test
    void normalizesHttpSchemes() {
        assertEquals("ws://platform/ws/game",
                WsSettings.of("http://platform/ws/game", "secret").endpoint().toString());
        assertEquals("wss://platform/ws/game",
                WsSettings.of("https://platform/ws/game", "secret").endpoint().toString());
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class, () -> WsSettings.of("ftp://platform/game", "s"));
    }

    @Test
    void disabledWhenUrlBlank() {
        WsSettings settings = WsSettings.of("", "secret");
        assertFalse(settings.enabled());
        assertThrows(IllegalStateException.class, settings::endpoint);
    }

    @Test
    void toStringRedactsSecret() {
        String text = WsSettings.of("ws://x", "super-secret").toString();
        assertTrue(text.contains("redacted"));
        assertFalse(text.contains("super-secret"));
    }
}
