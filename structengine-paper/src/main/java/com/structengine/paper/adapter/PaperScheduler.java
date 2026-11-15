package com.structengine.paper.adapter;

import com.structengine.buildmodel.spi.IServerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper 调度器：Bukkit 主线程投递 + 1 tick 周期任务。
 * <p>注意：Folia 等区域化调度服务端需替换为 RegionScheduler 实现。
 */
public class PaperScheduler implements IServerScheduler {

    private final JavaPlugin plugin;

    public PaperScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runOnServerThread(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void onServerTick(Runnable task) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, 1L, 1L);
    }
}
