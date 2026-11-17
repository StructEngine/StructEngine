package com.structengine.buildmodel.remote;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 游戏服到 Platform 的 WebSocket 客户端。
 *
 * <p>职责仅限网络层：连接、心跳、断线重连、消息编解码与回调桥接。
 * QUERY_Y 与 BUILD 的实际执行全部交给 {@link RemoteCommandBridge}，
 * 后者负责切换到游戏主线程，绝不在此线程直接调用 Minecraft API。</p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>JDK 原生 HttpClient WebSocket，无第三方网络依赖；</li>
 *   <li>心跳 20s；60s 未收到任何下行消息视为假死并主动重连；</li>
 *   <li>指数退避 1s → 2s → 4s ... 最大 30s；</li>
 *   <li>下行消息在独立虚拟线程执行器上解码/分发，不阻塞游戏主线程。</li>
 * </ul>
 */
public final class GameServerWsClient {

    private static final System.Logger LOG = System.getLogger("StructEngine.remote");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(20);
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(60);
    private static final long PLAYER_REFRESH_SECONDS = 10;
    private static final long MAX_RECONNECT_DELAY_SECONDS = 30;

    private final WsSettings settings;
    private final RemoteCommandBridge bridge;
    private final RemoteMessageCodec codec = new RemoteMessageCodec();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final Object connectionLock = new Object();
    private final Object sendLock = new Object();

    private volatile HttpClient httpClient;
    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile ScheduledExecutorService scheduler;
    private volatile ExecutorService virtualExecutor;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile ScheduledFuture<?> playerRefreshTask;
    private volatile long reconnectDelaySeconds = 1;
    private volatile long lastReceivedNanos = System.nanoTime();

    public GameServerWsClient(WsSettings settings, RemoteCommandBridge bridge) {
        if (settings == null) {
            throw new IllegalArgumentException("settings 不能为空");
        }
        if (bridge == null) {
            throw new IllegalArgumentException("bridge 不能为空");
        }
        this.settings = settings;
        this.bridge = bridge;
    }

    public WsSettings settings() {
        return settings;
    }

    public boolean isConnected() {
        return connected;
    }

    /** 启动连接；未配置 WS URL 时仅记录日志，不抛异常。 */
    public void start() {
        if (!settings.enabled()) {
            LOG.log(System.Logger.Level.INFO,
                    "未配置 STRUCTENGINE_PLATFORM_URL，远程构建网络层未启用");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "structengine-ws");
            thread.setDaemon(true);
            return thread;
        });
        virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .executor(virtualExecutor)
                .build();
        connect();
    }

    /** 停止连接并释放线程资源；可安全重复调用。 */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        connected = false;
        cancelHeartbeat();
        cancelPlayerRefresh();

        WebSocket socket;
        synchronized (connectionLock) {
            socket = webSocket;
            webSocket = null;
        }
        if (socket != null) {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "plugin shutdown");
            } catch (RuntimeException ignored) {
                // 连接已断开
            }
        }

        ScheduledExecutorService currentScheduler = scheduler;
        if (currentScheduler != null) {
            currentScheduler.shutdownNow();
        }
        ExecutorService currentExecutor = virtualExecutor;
        if (currentExecutor != null) {
            currentExecutor.shutdownNow();
        }
    }

    private void connect() {
        if (!running.get()) {
            return;
        }
        synchronized (connectionLock) {
            if (webSocket != null) {
                return;
            }
        }

        try {
            URI endpoint = settings.endpoint();
            WebSocket.Builder builder = httpClient.newWebSocketBuilder().connectTimeout(CONNECT_TIMEOUT);
            if (settings.secret() != null && !settings.secret().isBlank()) {
                builder.header("X-StructEngine-Secret", settings.secret());
            }
            LOG.log(System.Logger.Level.INFO, "正在连接 Platform WebSocket: " + endpoint);
            builder.buildAsync(endpoint, new Listener()).whenComplete((socket, error) -> {
                if (error != null) {
                    LOG.log(System.Logger.Level.WARNING,
                            "连接 Platform 失败: " + rootMessage(error) + "，稍后重连");
                    scheduleReconnect();
                } else {
                    onConnected(socket);
                }
            });
        } catch (Throwable e) {
            LOG.log(System.Logger.Level.WARNING, "创建 WebSocket 连接失败: " + rootMessage(e));
            scheduleReconnect();
        }
    }

    private void onConnected(WebSocket socket) {
        if (!running.get()) {
            socket.abort();
            return;
        }
        synchronized (connectionLock) {
            if (this.webSocket == socket) {
                return;
            }
            if (this.webSocket != null && this.webSocket != socket) {
                this.webSocket.abort();
            }
            this.webSocket = socket;
        }
        connected = true;
        reconnectDelaySeconds = 1;
        reconnectScheduled.set(false);
        lastReceivedNanos = System.nanoTime();
        scheduleHeartbeat();
        send(RemoteMessage.hello(settings.serverName(), settings.blueMapUrl(),
                settings.mapId(), settings.secret()));
        requestPlayers(true);
        schedulePlayerRefresh();
        LOG.log(System.Logger.Level.INFO,
                "已连接 Platform WebSocket serverName=" + settings.serverName());
    }

    private void handleDisconnect(WebSocket socket, String reason) {
        boolean wasConnected;
        synchronized (connectionLock) {
            if (socket != null && this.webSocket != socket) {
                return;
            }
            wasConnected = connected;
            connected = false;
            this.webSocket = null;
        }
        cancelHeartbeat();
        cancelPlayerRefresh();
        if (wasConnected) {
            LOG.log(System.Logger.Level.WARNING, "与 Platform 的连接断开: " + reason);
        }
        if (running.get()) {
            scheduleReconnect();
        }
    }

    private void scheduleHeartbeat() {
        synchronized (connectionLock) {
            cancelHeartbeat();
            ScheduledExecutorService current = scheduler;
            if (current != null && running.get()) {
                heartbeatTask = current.scheduleAtFixedRate(this::heartbeat,
                        HEARTBEAT_INTERVAL.toSeconds(), HEARTBEAT_INTERVAL.toSeconds(), TimeUnit.SECONDS);
            }
        }
    }

    private void cancelHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(false);
            heartbeatTask = null;
        }
    }

    private void schedulePlayerRefresh() {
        synchronized (connectionLock) {
            cancelPlayerRefresh();
            ScheduledExecutorService current = scheduler;
            if (current != null && running.get()) {
                playerRefreshTask = current.scheduleAtFixedRate(() -> requestPlayers(true),
                        PLAYER_REFRESH_SECONDS, PLAYER_REFRESH_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    private void cancelPlayerRefresh() {
        ScheduledFuture<?> task = playerRefreshTask;
        if (task != null) {
            task.cancel(false);
            playerRefreshTask = null;
        }
    }

    private void requestPlayers(boolean full) {
        if (!connected) {
            return;
        }
        bridge.players(players -> {
            if (connected) {
                send(RemoteMessage.playerList(players, full, settings.secret()));
            }
        }, error -> LOG.log(System.Logger.Level.WARNING,
                "读取玩家列表失败: " + rootMessage(error)));
    }

    /** 供平台事件监听器在玩家加入/离开/换世界时立即刷新。 */
    public void refreshPlayers() {
        requestPlayers(true);
    }

    private void heartbeat() {
        if (!running.get()) {
            return;
        }
        if (!connected) {
            scheduleReconnect();
            return;
        }
        long idleNanos = System.nanoTime() - lastReceivedNanos;
        if (idleNanos > HEARTBEAT_TIMEOUT.toNanos()) {
            WebSocket socket = webSocket;
            LOG.log(System.Logger.Level.WARNING,
                    "Platform 心跳超时（" + HEARTBEAT_TIMEOUT.toSeconds() + "s 无消息），主动重连");
            if (socket != null) {
                socket.abort();
            }
            handleDisconnect(socket, "heartbeat timeout");
            return;
        }
        send(RemoteMessage.ping(settings.secret()));
    }

    private void scheduleReconnect() {
        if (!running.get() || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }
        long delay = reconnectDelaySeconds;
        reconnectDelaySeconds = Math.min(reconnectDelaySeconds * 2, MAX_RECONNECT_DELAY_SECONDS);
        ScheduledExecutorService current = scheduler;
        if (current == null) {
            return;
        }
        try {
            current.schedule(() -> {
                reconnectScheduled.set(false);
                connect();
            }, delay, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
            reconnectScheduled.set(false);
        }
    }

    /** 发送消息；未连接时返回 false，不抛异常。 */
    public boolean send(RemoteMessage message) {
        if (!connected) {
            return false;
        }
        WebSocket socket = webSocket;
        if (socket == null) {
            return false;
        }
        try {
            String payload = codec.encode(message);
            synchronized (sendLock) {
                socket.sendText(payload, true).whenComplete((ignored, error) -> {
                    if (error != null) {
                        LOG.log(System.Logger.Level.WARNING,
                                "发送 " + message.action() + " 失败: " + rootMessage(error));
                    }
                });
            }
            return true;
        } catch (Throwable e) {
            LOG.log(System.Logger.Level.WARNING,
                    "发送 " + message.action() + " 异常: " + rootMessage(e));
            return false;
        }
    }

    /**
     * 处理一条完整下行 JSON。包级可见以便单元测试直接注入消息。
     */
    void handlePayload(String payload) {
        RemoteMessage message;
        try {
            message = codec.decode(payload);
        } catch (IllegalArgumentException e) {
            LOG.log(System.Logger.Level.WARNING, "忽略非法下行消息: " + e.getMessage());
            return;
        }
        lastReceivedNanos = System.nanoTime();

        switch (message.action()) {
            case QUERY_Y -> handleQuery(message);
            case BUILD -> handleBuild(message);
            case HISTORY_REQUEST -> handleHistory(message);
            case ROLLBACK -> handleRollback(message);
            case REDO -> handleRedo(message);
            case PING -> send(RemoteMessage.pong());
            case CONNECTED, PONG -> {
                // 连接确认 / 心跳回执
            }
            default -> LOG.log(System.Logger.Level.DEBUG,
                    "忽略 Platform 下行消息: " + message.action());
        }
    }

    private void handleQuery(RemoteMessage message) {
        String taskId = message.taskId();
        if (taskId == null || message.x() == null || message.z() == null) {
            LOG.log(System.Logger.Level.WARNING, "QUERY_Y 缺少 taskId/x/z，已忽略");
            return;
        }
        bridge.queryHighestY(taskId, message.x(), message.z(),
                y -> send(RemoteMessage.responseY(taskId, y, settings.secret())),
                error -> send(RemoteMessage.error(taskId, rootMessage(error), settings.secret())));
    }

    private void handleBuild(RemoteMessage message) {
        String taskId = message.taskId();
        if (taskId == null || message.playerName() == null
                || message.x() == null || message.y() == null || message.z() == null
                || message.modelUrl() == null) {
            String reason = "BUILD 缺少 taskId/playerName/坐标/modelUrl";
            LOG.log(System.Logger.Level.WARNING, reason + "，已忽略");
            send(RemoteMessage.error(taskId, reason, settings.secret()));
            return;
        }
        bridge.enqueueBuild(taskId, message.playerUuid(), message.playerName(),
                message.x(), message.y(), message.z(), message.modelUrl(),
                message.modelName(), message.rotation() == null ? 0 : message.rotation(),
                status -> send(RemoteMessage.buildStatus(taskId, message.playerUuid(),
                        status.state(), status.placed(), status.total(), status.message(), settings.secret())),
                error -> send(RemoteMessage.error(taskId, rootMessage(error), settings.secret())));
    }

    private void handleHistory(RemoteMessage message) {
        String requestId = message.requestId();
        if (requestId == null || message.playerName() == null) {
            LOG.log(System.Logger.Level.WARNING, "HISTORY_REQUEST 缺少 requestId/playerName");
            return;
        }
        bridge.history(requestId, message.playerUuid(), message.playerName(),
                history -> send(RemoteMessage.historyResponse(requestId, history, settings.secret())),
                error -> send(RemoteMessage.error(requestId, rootMessage(error), settings.secret())));
    }

    private void handleRollback(RemoteMessage message) {
        if (message.requestId() == null || message.playerName() == null) {
            return;
        }
        bridge.rollback(message.requestId(), message.playerUuid(), message.playerName(),
                message.historyId(),
                status -> send(RemoteMessage.buildStatus(message.requestId(), message.playerUuid(),
                        status.state(), status.placed(), status.total(), status.message(), settings.secret())),
                error -> send(RemoteMessage.error(message.requestId(), rootMessage(error), settings.secret())));
    }

    private void handleRedo(RemoteMessage message) {
        if (message.requestId() == null || message.playerName() == null) {
            return;
        }
        bridge.redo(message.requestId(), message.playerUuid(), message.playerName(),
                message.historyId(),
                status -> send(RemoteMessage.buildStatus(message.requestId(), message.playerUuid(),
                        status.state(), status.placed(), status.total(), status.message(), settings.secret())),
                error -> send(RemoteMessage.error(message.requestId(), rootMessage(error), settings.secret())));
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }

    private final class Listener implements WebSocket.Listener {

        private final StringBuilder partial = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) {
            onConnected(socket);
            socket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String payload = partial.toString();
                partial.setLength(0);
                try {
                    handlePayload(payload);
                } catch (Throwable e) {
                    LOG.log(System.Logger.Level.ERROR, "处理下行消息异常: " + rootMessage(e));
                }
            }
            socket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            handleDisconnect(socket, "closed " + statusCode + " " + reason);
            return null;
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            LOG.log(System.Logger.Level.WARNING, "WebSocket 异常: " + rootMessage(error));
            handleDisconnect(socket, "error: " + rootMessage(error));
        }
    }
}
