package com.structengine.buildmodel.remote;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameServerWsClientTest {

    private final RecordingBridge bridge = new RecordingBridge();
    private final GameServerWsClient client = new GameServerWsClient(
            WsSettings.of("ws://localhost:1/ws/game", "survival", ""), bridge);

    @Test
    void queryYDispatchesToBridgeOnMainThreadContract() {
        client.handlePayload("{\"action\":\"QUERY_Y\",\"taskId\":\"t-1\",\"playerName\":\"Steve\",\"x\":10,\"z\":-20}");

        assertEquals("t-1", bridge.queryTaskId.get());
        assertEquals(10, bridge.queryX.get());
        assertEquals(-20, bridge.queryZ.get());
        assertEquals(88, bridge.returnedY.get());
    }

    @Test
    void buildDispatchesFullAbsoluteCoordinates() {
        client.handlePayload("{\"action\":\"BUILD\",\"taskId\":\"t-2\",\"playerName\":\"Alex\","
                + "\"x\":1,\"y\":64,\"z\":-3,\"modelUrl\":\"http://platform/models/t-2.json\"}");

        assertEquals("t-2", bridge.buildTaskId.get());
        assertEquals("Alex", bridge.buildPlayer.get());
        assertEquals(1, bridge.buildX.get());
        assertEquals(64, bridge.buildY.get());
        assertEquals(-3, bridge.buildZ.get());
        assertEquals("http://platform/models/t-2.json", bridge.buildUrl.get());
    }

    @Test
    void malformedOrIncompleteMessagesAreIgnoredSafely() {
        client.handlePayload("not-json");
        client.handlePayload("{\"action\":\"QUERY_Y\",\"taskId\":\"t\"}");
        client.handlePayload("{\"action\":\"BUILD\",\"taskId\":\"t\"}");
        client.handlePayload("{\"action\":\"PING\"}");

        assertNull(bridge.queryTaskId.get());
        assertNull(bridge.buildTaskId.get());
        assertFalse(client.isConnected());
    }

    @Test
    void sendReturnsFalseWhileDisconnected() {
        assertFalse(client.send(RemoteMessage.ping()));
    }

    @Test
    void stopIsIdempotentBeforeStart() {
        client.stop();
        client.stop();
        assertFalse(client.isConnected());
    }

    private static final class RecordingBridge implements RemoteCommandBridge {
        final AtomicReference<String> queryTaskId = new AtomicReference<>();
        final AtomicReference<Integer> queryX = new AtomicReference<>();
        final AtomicReference<Integer> queryZ = new AtomicReference<>();
        final AtomicReference<Integer> returnedY = new AtomicReference<>();
        final AtomicReference<String> buildTaskId = new AtomicReference<>();
        final AtomicReference<String> buildPlayer = new AtomicReference<>();
        final AtomicReference<Integer> buildX = new AtomicReference<>();
        final AtomicReference<Integer> buildY = new AtomicReference<>();
        final AtomicReference<Integer> buildZ = new AtomicReference<>();
        final AtomicReference<String> buildUrl = new AtomicReference<>();

        @Override
        public void queryHighestY(String taskId, int x, int z, Consumer<Integer> onY, Consumer<Throwable> onError) {
            queryTaskId.set(taskId);
            queryX.set(x);
            queryZ.set(z);
            returnedY.set(88);
            onY.accept(88);
        }

        @Override
        public void enqueueBuild(String taskId, String playerUuid, String playerName,
                                 int x, int y, int z, String modelUrl, String modelName, int rotation,
                                 Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
            buildTaskId.set(taskId);
            buildPlayer.set(playerName);
            buildX.set(x);
            buildY.set(y);
            buildZ.set(z);
            buildUrl.set(modelUrl);
        }
    }
}
