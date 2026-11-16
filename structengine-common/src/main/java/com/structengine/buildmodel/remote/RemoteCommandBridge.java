package com.structengine.buildmodel.remote;

import java.util.List;
import java.util.function.Consumer;

/**
 * WS 网络层与游戏主线程之间的桥接接口。
 *
 * <p>实现方必须保证：所有 Minecraft API 调用都通过 {@code IServerScheduler}
 * 投递到服务端主线程；网络线程只负责解析与回调。</p>
 */
public interface RemoteCommandBridge {

    void queryHighestY(String taskId, int x, int z, Consumer<Integer> onY, Consumer<Throwable> onError);

    /**
     * 在主线程读取在线玩家列表。
     */
    default void players(Consumer<List<RemotePlayerInfo>> onPlayers, Consumer<Throwable> onError) {
        onPlayers.accept(List.of());
    }

    /**
     * 接收 BUILD：使用 Platform 指定的 UUID/玩家名，按绝对原点构建。
     */
    default void enqueueBuild(String taskId, String playerUuid, String playerName,
                              int x, int y, int z, String modelUrl, String modelName, int rotation,
                              Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
        enqueueBuild(taskId, playerName, x, y, z, modelUrl, onError);
    }

    /**
     * 读取指定玩家的构建历史。
     */
    default void history(String requestId, String playerUuid, String playerName,
                         Consumer<List<RemoteHistoryEntry>> onHistory, Consumer<Throwable> onError) {
        onError.accept(new UnsupportedOperationException("history 尚未接入核心历史管理器"));
    }

    /**
     * 回滚指定历史记录。
     */
    default void rollback(String requestId, String playerUuid, String playerName, String historyId,
                          Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
        onError.accept(new UnsupportedOperationException("rollback 尚未接入核心历史管理器"));
    }

    /**
     * 恢复指定历史记录。
     */
    default void redo(String requestId, String playerUuid, String playerName, String historyId,
                      Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
        onError.accept(new UnsupportedOperationException("redo 尚未接入核心历史管理器"));
    }

    /** 兼容旧调用。 */
    default void enqueueBuild(String taskId, String playerName, int x, int y, int z,
                              String modelUrl, Consumer<Throwable> onError) {
        enqueueBuild(taskId, null, playerName, x, y, z, modelUrl, taskId, 0,
                status -> { }, onError);
    }
}
