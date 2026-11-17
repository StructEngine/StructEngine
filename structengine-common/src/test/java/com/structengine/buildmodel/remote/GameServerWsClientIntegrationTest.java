package com.structengine.buildmodel.remote;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实网络环回测试：Java-WebSocket 充当 Platform 服务端，JDK HttpClient 客户端连接。
 */
class GameServerWsClientIntegrationTest {

    @Test
    void connectsAuthenticatesAndExchangesProtocolMessages() throws Exception {
        int port;
        try (ServerSocket probe = new ServerSocket(0)) {
            port = probe.getLocalPort();
        }
        TestServer server = new TestServer(port);
        server.start();
        try (TestClient client = new TestClient(port)) {
            assertTrue(server.connected.await(5, TimeUnit.SECONDS), "服务端应收到连接");
            waitUntil(() -> client.client.isConnected(), 5000);
            assertEquals("secret", server.secret.get());
            assertEquals("/ws/game", server.resource.get());

            // Platform 下发 QUERY_Y，客户端必须回调 bridge 并回传 RESPONSE_Y。
            server.send("{\"action\":\"QUERY_Y\",\"taskId\":\"q-1\",\"playerName\":\"Steve\",\"x\":10,\"z\":-20}");
            assertTrue(client.bridge.queryLatch.await(5, TimeUnit.SECONDS));
            assertEquals("q-1", client.bridge.queryTaskId.get());
            assertEquals(10, client.bridge.queryX.get());
            assertEquals(-20, client.bridge.queryZ.get());

            String response = pollContaining(server.messages, "\"action\":\"RESPONSE_Y\"", 5000);
            assertTrue(response.contains("\"taskId\":\"q-1\""));
            assertTrue(response.contains("\"y\":72"));

            // Platform 下发 BUILD，客户端必须把绝对坐标完整交给 bridge。
            server.send("{\"action\":\"BUILD\",\"taskId\":\"b-1\",\"playerName\":\"Alex\","
                    + "\"x\":1,\"y\":64,\"z\":-3,\"modelUrl\":\"http://platform/models/b-1.json\"}");
            assertTrue(client.bridge.buildLatch.await(5, TimeUnit.SECONDS));
            assertEquals("b-1", client.bridge.buildTaskId.get());
            assertEquals(1, client.bridge.buildX.get());
            assertEquals(64, client.bridge.buildY.get());
            assertEquals(-3, client.bridge.buildZ.get());
            assertEquals("http://platform/models/b-1.json", client.bridge.buildUrl.get());

            // PING → PONG 心跳回执。
            server.send("{\"action\":\"PING\"}");
            String pong = pollContaining(server.messages, "\"action\":\"PONG\"", 5000);
            assertNotNull(pong);
        } finally {
            server.stop(1000);
        }
    }

    private static String pollContaining(BlockingQueue<String> queue, String needle, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String message = queue.poll(50, TimeUnit.MILLISECONDS);
            if (message != null && message.contains(needle)) {
                return message;
            }
        }
        return null;
    }

    private static void waitUntil(Check check, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("条件在超时前未满足");
    }

    private interface Check {
        boolean ok();
    }

    private static final class TestServer extends WebSocketServer {

        final CountDownLatch connected = new CountDownLatch(1);
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final AtomicReference<String> secret = new AtomicReference<>();
        final AtomicReference<String> resource = new AtomicReference<>();
        private volatile WebSocket socket;

        TestServer(int port) {
            super(new InetSocketAddress("127.0.0.1", port));
            setReuseAddr(true);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            socket = conn;
            secret.set(handshake.getFieldValue("X-StructEngine-Secret"));
            resource.set(handshake.getResourceDescriptor());
            connected.countDown();
            conn.send("{\"action\":\"CONNECTED\",\"message\":\"ok\"}");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            messages.add(message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
        }

        @Override
        public void onStart() {
        }

        void send(String payload) {
            socket.send(payload);
        }
    }

    private static final class TestClient implements AutoCloseable {

        final GameServerWsClient client;
        final TestBridge bridge = new TestBridge();

        TestClient(int port) {
            client = new GameServerWsClient(
                    WsSettings.of("ws://127.0.0.1:" + port + "/ws/game", "secret"),
                    bridge);
            client.start();
        }

        @Override
        public void close() {
            client.stop();
        }
    }

    private static final class TestBridge implements RemoteCommandBridge {

        final CountDownLatch queryLatch = new CountDownLatch(1);
        final CountDownLatch buildLatch = new CountDownLatch(1);
        final AtomicReference<String> queryTaskId = new AtomicReference<>();
        final AtomicReference<Integer> queryX = new AtomicReference<>();
        final AtomicReference<Integer> queryZ = new AtomicReference<>();
        final AtomicReference<String> buildTaskId = new AtomicReference<>();
        final AtomicReference<Integer> buildX = new AtomicReference<>();
        final AtomicReference<Integer> buildY = new AtomicReference<>();
        final AtomicReference<Integer> buildZ = new AtomicReference<>();
        final AtomicReference<String> buildUrl = new AtomicReference<>();

        @Override
        public void queryHighestY(String taskId, int x, int z, Consumer<Integer> onY, Consumer<Throwable> onError) {
            queryTaskId.set(taskId);
            queryX.set(x);
            queryZ.set(z);
            queryLatch.countDown();
            onY.accept(72);
        }

        @Override
        public void enqueueBuild(String taskId, String playerName, int x, int y, int z,
                                 String modelUrl, Consumer<Throwable> onError) {
            buildTaskId.set(taskId);
            buildX.set(x);
            buildY.set(y);
            buildZ.set(z);
            buildUrl.set(modelUrl);
            buildLatch.countDown();
        }
    }
}
