package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.spi.IServerScheduler;

/**
 * 服务端 tick 驱动器：每 tick 最多放置 {@link #MAX_BLOCKS_PER_TICK} 个方块。
 * <p>平台无关：tick 事件由 {@link IServerScheduler#onServerTick(Runnable)} 提供。
 */
public class BuildTickHandler {
    public static final int MAX_BLOCKS_PER_TICK = 1000;

    private BuildTickHandler() {
    }

    public static void register(IServerScheduler scheduler, BuildTaskQueue taskQueue) {
        scheduler.onServerTick(() -> taskQueue.tick(MAX_BLOCKS_PER_TICK));
    }
}
