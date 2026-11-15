package com.structengine.buildmodel.remote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteMessageCodecTest {

    private final RemoteMessageCodec codec = new RemoteMessageCodec();

    @Test
    void encodesQueryYWithoutNulls() {
        String json = codec.encode(RemoteMessage.queryY("t-1", "Steve", 10, -20));
        assertTrue(json.contains("\"action\":\"QUERY_Y\""));
        assertTrue(json.contains("\"taskId\":\"t-1\""));
        assertTrue(json.contains("\"x\":10"));
        assertTrue(json.contains("\"z\":-20"));
        assertFalse(json.contains("\"y\""));
        assertFalse(json.contains("modelUrl"));
    }

    @Test
    void decodesBuildFromPlatform() {
        RemoteMessage message = codec.decode(
                "{\"action\":\"BUILD\",\"taskId\":\"t\",\"playerName\":\"Alex\","
                        + "\"x\":1,\"y\":64,\"z\":-3,\"modelUrl\":\"http://x/m.json\"}");
        assertEquals(RemoteAction.BUILD, message.action());
        assertEquals("Alex", message.playerName());
        assertEquals(64, message.y());
        assertEquals("http://x/m.json", message.modelUrl());
    }

    @Test
    void ignoresUnknownFields() {
        RemoteMessage message = codec.decode("{\"action\":\"PING\",\"extra\":true}");
        assertEquals(RemoteAction.PING, message.action());
    }

    @Test
    void rejectsMissingActionOrMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> codec.decode("{\"taskId\":\"t\"}"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("not-json"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode(""));
    }
}
