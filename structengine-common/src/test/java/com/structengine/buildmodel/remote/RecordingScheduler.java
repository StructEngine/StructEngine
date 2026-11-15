package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.spi.IServerScheduler;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 测试调度器：只记录投递任务，由测试显式 drain，以验证主线程切换。
 */
final class RecordingScheduler implements IServerScheduler {

    private final Deque<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void runOnServerThread(Runnable task) {
        tasks.add(task);
    }

    @Override
    public void onServerTick(Runnable task) {
        // 不参与本测试
    }

    int pending() {
        return tasks.size();
    }

    void runAll() {
        while (!tasks.isEmpty()) {
            tasks.poll().run();
        }
    }
}
