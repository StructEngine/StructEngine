package com.structengine.buildmodel.service;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.queue.BuildRecord;
import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.queue.ModelBuildJob;
import com.structengine.buildmodel.queue.RollbackJob;
import com.structengine.buildmodel.queue.RedoJob;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BuildHistoryManager {
    private static final BuildHistoryManager INSTANCE = new BuildHistoryManager();
    private static final int MAX_HISTORY = 5;

    private final Map<UUID, PlayerHistory> histories = new HashMap<>();

    private BuildHistoryManager() {
    }

    public static BuildHistoryManager getInstance() {
        return INSTANCE;
    }

    private PlayerHistory getOrCreate(UUID playerId) {
        return histories.computeIfAbsent(playerId, k -> new PlayerHistory());
    }

    public void recordBuild(ServerPlayerEntity player, World world, RegionSnapshot snapshot,
                            List<BlockPlacement> placements, ModelBuildJob job) {
        PlayerHistory history = getOrCreate(player.getUuid());
        BuildRecord record = new BuildRecord(world, snapshot, placements, job);
        history.undoStack.push(record);
        history.redoStack.clear();
        while (history.undoStack.size() > MAX_HISTORY) {
            history.undoStack.removeLast();
        }
    }

    public void rollback(ServerPlayerEntity player) {
        PlayerHistory history = getOrCreate(player.getUuid());
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
            BuildTaskQueue.getInstance().remove(record.getAssociatedJob());
        }
        history.restoreInProgress = true;
        RollbackJob job = new RollbackJob(player, record.getWorld(), record.getSnapshot());
        BuildTaskQueue.getInstance().enqueue(job);
        history.redoStack.push(record);
        sendInfo(player, "正在回滚 " + record.getSnapshot().getTotal() + " 个方块...");
    }

    public void redo(ServerPlayerEntity player) {
        PlayerHistory history = getOrCreate(player.getUuid());
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
        RedoJob job = new RedoJob(player, record.getWorld(), record.getPlacements());
        BuildTaskQueue.getInstance().enqueue(job);
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

    public int undoCount(ServerPlayerEntity player) {
        PlayerHistory history = histories.get(player.getUuid());
        return history == null ? 0 : history.undoStack.size();
    }

    public int redoCount(ServerPlayerEntity player) {
        PlayerHistory history = histories.get(player.getUuid());
        return history == null ? 0 : history.redoStack.size();
    }

    private void sendInfo(ServerPlayerEntity player, String message) {
        if (player.isDisconnected()) {
            return;
        }
        player.sendMessage(Text.literal(message).withColor(0xFFFFFF55));
    }

    private void sendError(ServerPlayerEntity player, String message) {
        if (player.isDisconnected()) {
            return;
        }
        player.sendMessage(Text.literal("[StructEngine] " + message).withColor(0xFFFF5555));
    }

    private static class PlayerHistory {
        final Deque<BuildRecord> undoStack = new ArrayDeque<>();
        final Deque<BuildRecord> redoStack = new ArrayDeque<>();
        boolean restoreInProgress = false;
    }
}
