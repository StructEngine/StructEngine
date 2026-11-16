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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 每个玩家的构建历史（撤销/重做）管理。
 *
 * <p>历史保存在内存中，重启后清空；Web 端只展示当前进程可操作的历史。</p>
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

    /** 兼容旧调用。 */
    public String recordBuild(PlayerRef player, WorldRef world, RegionSnapshot snapshot,
                              List<BlockPlacement> placements, ModelBuildJob job) {
        return recordBuild(player, world, snapshot, placements, job, null, null);
    }

    public String recordBuild(PlayerRef player, WorldRef world, RegionSnapshot snapshot,
                              List<BlockPlacement> placements, ModelBuildJob job,
                              String taskId, String modelName) {
        PlayerHistory history = getOrCreate(player.id());
        BuildRecord record = new BuildRecord(UUID.randomUUID().toString(), taskId, modelName,
                System.currentTimeMillis(), world, snapshot, placements, job);
        history.undoStack.push(record);
        history.redoStack.clear();
        while (history.undoStack.size() > MAX_HISTORY) {
            history.undoStack.removeLast();
        }
        return record.getHistoryId();
    }

    /** 回滚该玩家最近一次构建。 */
    public void rollback(PlayerRef player) {
        PlayerHistory history = getOrCreate(player.id());
        if (history.undoStack.isEmpty()) {
            sendError(player, "没有可回滚的构建。");
            return;
        }
        rollback(player, history.undoStack.peek().getHistoryId());
    }

    /** 回滚指定历史记录。 */
    public boolean rollback(PlayerRef player, String historyId) {
        PlayerHistory history = getOrCreate(player.id());
        if (history.restoreInProgress) {
            sendError(player, "上一次回滚/恢复仍在进行中，请稍候。");
            return false;
        }
        BuildRecord record = find(history.undoStack, historyId);
        if (record == null) {
            sendError(player, "找不到可回滚的历史记录。");
            return false;
        }
        history.undoStack.remove(record);
        if (record.getAssociatedJob() != null) {
            taskQueue.remove(record.getAssociatedJob());
        }
        history.restoreInProgress = true;
        RollbackJob job = new RollbackJob(player, record.getWorld(), record.getSnapshot(), this, worldAdapter);
        taskQueue.enqueue(job);
        history.redoStack.push(record);
        sendInfo(player, "正在回滚 " + record.getSnapshot().getTotal() + " 个方块...");
        return true;
    }

    /** 恢复该玩家最近一次回滚。 */
    public void redo(PlayerRef player) {
        PlayerHistory history = getOrCreate(player.id());
        if (history.redoStack.isEmpty()) {
            sendError(player, "没有可恢复的构建。");
            return;
        }
        redo(player, history.redoStack.peek().getHistoryId());
    }

    /** 恢复指定历史记录。 */
    public boolean redo(PlayerRef player, String historyId) {
        PlayerHistory history = getOrCreate(player.id());
        if (history.restoreInProgress) {
            sendError(player, "上一次回滚/恢复仍在进行中，请稍候。");
            return false;
        }
        BuildRecord record = find(history.redoStack, historyId);
        if (record == null) {
            sendError(player, "找不到可恢复的历史记录。");
            return false;
        }
        history.redoStack.remove(record);
        history.restoreInProgress = true;
        RedoJob job = new RedoJob(player, record.getWorld(), record.getPlacements(), this, worldAdapter);
        taskQueue.enqueue(job);
        history.undoStack.push(record);
        while (history.undoStack.size() > MAX_HISTORY) {
            history.undoStack.removeLast();
        }
        sendInfo(player, "正在恢复 " + record.getPlacements().size() + " 个方块...");
        return true;
    }

    public List<BuildHistoryView> history(PlayerRef player) {
        PlayerHistory history = histories.get(player.id());
        if (history == null) {
            return List.of();
        }
        List<BuildHistoryView> result = new ArrayList<>();
        for (BuildRecord record : history.undoStack) {
            result.add(view(record, true, false));
        }
        for (BuildRecord record : history.redoStack) {
            result.add(view(record, false, true));
        }
        result.sort(Comparator.comparingLong(BuildHistoryView::createdAt).reversed());
        return result;
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

    private static BuildHistoryView view(BuildRecord record, boolean canRollback, boolean canRedo) {
        return new BuildHistoryView(record.getHistoryId(), record.getTaskId(), record.getModelName(),
                record.getCreatedAt(), record.getPlacements().size(), canRollback, canRedo);
    }

    private static BuildRecord find(Deque<BuildRecord> records, String historyId) {
        for (BuildRecord record : records) {
            if (record.getHistoryId().equals(historyId)) {
                return record;
            }
        }
        return null;
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
