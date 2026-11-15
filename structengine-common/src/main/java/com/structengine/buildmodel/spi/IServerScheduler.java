package com.structengine.buildmodel.spi;

/**
 * 服务端调度器：抽象原实现中的主线程投递与 Server Tick 回调注册。
 * <p>Fabric 用 {@code ServerTickEvents.END_SERVER_TICK} + {@code server.execute}，
 * NeoForge 用 {@code ServerTickEvent.Post}，Paper 用 Bukkit Scheduler。
 */
public interface IServerScheduler {

    /** 投递到服务端主线程执行（原 {@code server.execute(...)}）。 */
    void runOnServerThread(Runnable task);

    /** 注册每个服务端 tick 结束时的回调（原 {@code ServerTickEvents.END_SERVER_TICK}）。 */
    void onServerTick(Runnable task);
}
