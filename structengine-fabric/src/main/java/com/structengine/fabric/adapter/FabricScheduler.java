package com.structengine.fabric.adapter;

import com.structengine.buildmodel.spi.IServerScheduler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/**
 * Fabric 调度器：主线程投递 + END_SERVER_TICK 回调。
 */
public class FabricScheduler implements IServerScheduler {

    private volatile MinecraftServer server;

    public FabricScheduler() {
        ServerLifecycleEvents.SERVER_STARTED.register(started -> this.server = started);
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> this.server = null);
    }

    /** 当前服务端实例；未启动时为 null。 */
    public MinecraftServer server() {
        return server;
    }

    @Override
    public void runOnServerThread(Runnable task) {
        MinecraftServer current = this.server;
        if (current == null) {
            throw new IllegalStateException("服务端尚未就绪，无法投递主线程任务");
        }
        current.execute(task);
    }

    @Override
    public void onServerTick(Runnable task) {
        ServerTickEvents.END_SERVER_TICK.register(server -> task.run());
    }
}
