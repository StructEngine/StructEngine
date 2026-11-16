package com.structengine.buildmodel.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 多玩家构建队列：每个玩家一条独立 FIFO，按玩家轮转执行。
 *
 * <p>不同玩家可以并行推进，但共享每个 tick 的方块放置预算，避免同一 tick 放置过多方块。
 * 未归属玩家的旧任务进入全局 FIFO，保持与旧版本兼容。</p>
 */
public class BuildTaskQueue {

    private final Queue<BuildJob> unowned = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Queue<BuildJob>> playerQueues = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<UUID> playerOrder = new CopyOnWriteArrayList<>();

    public void enqueue(BuildJob job) {
        UUID playerId = job.playerId();
        if (playerId == null) {
            unowned.add(job);
            return;
        }
        Queue<BuildJob> queue = playerQueues.computeIfAbsent(playerId, id -> new ConcurrentLinkedQueue<>());
        if (!playerOrder.contains(playerId)) {
            playerOrder.add(playerId);
        }
        queue.add(job);
    }

    public boolean remove(BuildJob job) {
        if (job.playerId() == null) {
            return unowned.remove(job);
        }
        Queue<BuildJob> queue = playerQueues.get(job.playerId());
        boolean removed = queue != null && queue.remove(job);
        if (removed) {
            cleanup(job.playerId());
        }
        return removed;
    }

    public boolean isEmpty() {
        if (!unowned.isEmpty()) {
            return false;
        }
        for (Queue<BuildJob> queue : playerQueues.values()) {
            if (!queue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int pendingJobs() {
        int count = unowned.size();
        for (Queue<BuildJob> queue : playerQueues.values()) {
            count += queue.size();
        }
        return count;
    }

    /**
     * 按玩家轮转执行，每个玩家每轮最多放置一个方块，共享每个 tick 的预算。
     */
    public int tick(int maxBlocksPerTick) {
        if (maxBlocksPerTick <= 0) {
            return 0;
        }
        List<Queue<BuildJob>> queues = new ArrayList<>();
        queues.add(unowned);
        for (UUID playerId : new ArrayList<>(playerOrder)) {
            Queue<BuildJob> queue = playerQueues.get(playerId);
            if (queue != null) {
                queues.add(queue);
            }
        }

        int placed = 0;
        while (placed < maxBlocksPerTick) {
            boolean progressed = false;
            boolean empty = true;
            for (Queue<BuildJob> queue : queues) {
                if (placed >= maxBlocksPerTick) {
                    break;
                }
                if (queue.isEmpty()) {
                    continue;
                }
                empty = false;
                BuildJob job = queue.peek();
                try {
                    if (job.processNext()) {
                        placed++;
                        progressed = true;
                    }
                } catch (RuntimeException e) {
                    queue.poll();
                    job.notifyAborted(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    progressed = true;
                }
                if (job.isDone()) {
                    queue.poll();
                    job.notifyComplete();
                }
            }
            if (empty) {
                break;
            }
            if (!progressed) {
                break;
            }
        }
        for (UUID playerId : new ArrayList<>(playerOrder)) {
            cleanup(playerId);
        }
        return placed;
    }

    private int processQueue(Queue<BuildJob> queue, int budget) {
        int placed = 0;
        while (placed < budget && !queue.isEmpty()) {
            BuildJob job = queue.peek();
            try {
                while (placed < budget && !job.isDone()) {
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

    private void cleanup(UUID playerId) {
        Queue<BuildJob> queue = playerQueues.get(playerId);
        if (queue != null && queue.isEmpty()) {
            playerQueues.remove(playerId, queue);
            playerOrder.remove(playerId);
        }
    }
}
