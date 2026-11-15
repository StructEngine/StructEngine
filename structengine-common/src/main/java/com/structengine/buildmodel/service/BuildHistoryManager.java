package com.structengine.buildmodel.service;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.queue.BuildRecord;
import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.queue.ModelBuildJob;
import com.structengine.buildmodel.queue.RedoJob;
import com.structengine.buildmodel.queue.RollbackJob;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 每个玩家的构建历史（撤销/重做）管理。
 * <p>平台无关：世界与玩家通过 {@code WorldRef} / {@code PlayerRef} 传入。
 */
public class BuildHistoryManager {
    private static final int MAX_HISTORY = 5;

    private final Map<UUID, PlayerHistory> histories = new HashMap<>();
    private final IWorldAdapter worldAdapter;
    private final BuildTaskQueue taskQueue;

    public BuildHistoryManager(IWorldAdapter worldAdapter, BuildTaskQueue taskQueue) {
        this.worldAdapter = worldAdapter;
        this.taskQueue = taskQueue;
    }

    private PlayerHistory getOrCreate(UUID playerId) {
        return histories.computeIfAbsent(playerId, k -> new PlayerHistory());
    }

    public void recordBuild(PlayerRef player, WorldRef world, RegionSnapshot snapshot,
                            List<BlockPlacement> placements, ModelBuildJob job) {
        PlayerHistory history = getOrCreate(player.id());
        BuildRecord record = new BuildRecord(world, snapshot, placements, job);
        history.undoStack.push(record);
        history.redoStack.clear();
        while (history.undoStack.size() > MAX_HISTORY) {
            history.undoStack.removeLast();
        }
    }

    public void rollback(PlayerRef player) {
        PlayerHistory history = getOrCreate(player.id());
        if (history.restoreInProgress) {
            sendError(player, "上一次回滚/恢复仍在进行中，请稍候。");
            return;
        }
        if (history.undoStack.isEmpty()) {
            sendError(player, "没有可回滚的构建。");
            return;
        }
        BuildRecord record = history.undoStack.pop();
        if (record.getAssociatedJob() != null) {
            taskQueue.remove(record.getAssociatedJob());
        }
        history.restoreInProgress = true;
        RollbackJob job = new RollbackJob(player, record.getWorld(), record.getSnapshot(), this, worldAdapter);
        taskQueue.enqueue(job);
        history.redoStack.push(record);
        sendInfo(player, "正在回滚 " + record.getSnapshot().getTotal() + " 个方块...");
    }

    public void redo(PlayerRef player) {
        PlayerHistory history = getOrCreate(player.id());
        if (history.restoreInProgress) {
            sendError(player, "上一次回滚/恢复仍在进行中，请稍候。");
            return;
        }
        if (history.redoStack.isEmpty()) {
            sendError(player, "没有可恢复的构建。");
            return;
        }
        BuildRecord record = history.redoStack.pop();
        history.restoreInProgress = true;
        RedoJob job = new RedoJob(player, record.getWorld(), record.getPlacements(), this, worldAdapter);
        taskQueue.enqueue(job);
        history.undoStack.push(record);
        while (history.undoStack.size() > MAX_HISTORY) {
            history.undoStack.removeLast();
        }
        sendInfo(player, "正在恢复 " + record.getPlacements().size() + " 个方块...");
    }

    public void onRestoreComplete(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history != null) {
            history.restoreInProgress = false;
        }
    }

    public int undoCount(PlayerRef player) {
        PlayerHistory history = histories.get(player.id());
        return history == null ? 0 : history.undoStack.size();
    }

    public int redoCount(PlayerRef player) {
        PlayerHistory history = histories.get(player.id());
        return history == null ? 0 : history.redoStack.size();
    }

    private void sendInfo(PlayerRef player, String message) {
        if (!worldAdapter.isOnline(player)) {
            return;
        }
        worldAdapter.sendMessage(player, message, 0xFFFFFF55);
    }

    private void sendError(PlayerRef player, String message) {
        if (!worldAdapter.isOnline(player)) {
            return;
        }
        worldAdapter.sendMessage(player, "[StructEngine] " + message, 0xFFFF5555);
    }

    private static class PlayerHistory {
        final Deque<BuildRecord> undoStack = new ArrayDeque<>();
        final Deque<BuildRecord> redoStack = new ArrayDeque<>();
        boolean restoreInProgress = false;
    }
}
