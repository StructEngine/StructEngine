package com.structengine.buildmodel.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 构建任务队列：FIFO 依次执行，每个 tick 由 {@link BuildTickHandler} 驱动。
 */
public class BuildTaskQueue {

    private final Queue<BuildJob> queue = new ConcurrentLinkedQueue<>();

    public BuildTaskQueue() {
    }

    public void enqueue(BuildJob job) {
        queue.add(job);
    }

    public boolean remove(BuildJob job) {
        return queue.remove(job);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int pendingJobs() {
        return queue.size();
    }

    public int tick(int maxBlocksPerTick) {
        int placed = 0;
        while (placed < maxBlocksPerTick && !queue.isEmpty()) {
            BuildJob job = queue.peek();
            try {
                while (placed < maxBlocksPerTick && !job.isDone()) {
                    if (job.processNext()) {
                        placed++;
                    }
                }
            } catch (RuntimeException e) {
                queue.poll();
                job.notifyAborted(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                continue;
            }
            if (job.isDone()) {
                queue.poll();
                job.notifyComplete();
            }
        }
        return placed;
    }
}
