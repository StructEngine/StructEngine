package com.structengine.neoforge.adapter;

import com.structengine.buildmodel.spi.IServerScheduler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge 调度器：主线程投递 + ServerTickEvent.Post 回调。
 */
public class NeoForgeScheduler implements IServerScheduler {

    private volatile MinecraftServer server;

    public void bind(MinecraftServer server) {
        this.server = server;
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
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> task.run());
    }
}
