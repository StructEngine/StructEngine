package com.realcraft.buildmodel.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BuildTaskQueue {
    private static final BuildTaskQueue INSTANCE = new BuildTaskQueue();

    private final Queue<BuildJob> queue = new ConcurrentLinkedQueue<>();

    private BuildTaskQueue() {
    }

    public static BuildTaskQueue getInstance() {
        return INSTANCE;
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
