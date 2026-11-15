package com.structengine.buildmodel.remote;

import java.util.function.Consumer;

/**
 * WS 网络层与游戏主线程之间的桥接接口。
 *
 * <p>实现方必须保证：所有 Minecraft API 调用都通过 {@code IServerScheduler}
 * 投递到服务端主线程；网络线程只负责解析与回调。</p>
 */
public interface RemoteCommandBridge {

    /**
     * 在主线程查询 {@code (x,z)} 的地表高度，查询完成后通过回调返回。
     */
    void queryHighestY(String taskId, int x, int z, Consumer<Integer> onY, Consumer<Throwable> onError);

    /**
     * 接收 BUILD：在主线程解析玩家/世界并启动异步下载，下载完成后压入原有任务队列。
     */
    void enqueueBuild(String taskId, String playerName, int x, int y, int z,
                      String modelUrl, Consumer<Throwable> onError);
}
