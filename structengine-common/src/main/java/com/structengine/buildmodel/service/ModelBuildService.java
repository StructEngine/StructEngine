package com.structengine.buildmodel.service;

import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.queue.ModelBuildJob;
import com.structengine.buildmodel.spi.IServerScheduler;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 构建流程编排：下载 -> 解析 -> 生成任务 -> 入队。
 * <p>主线程投递通过 {@link IServerScheduler}，世界操作通过 {@link IWorldAdapter}。
 */
public class ModelBuildService {

    private final ModelDownloader downloader;
    private final IWorldAdapter worldAdapter;
    private final IServerScheduler scheduler;
    private final BuildHistoryManager historyManager;
    private final BuildTaskQueue taskQueue;

    public ModelBuildService(ModelDownloader downloader, IWorldAdapter worldAdapter, IServerScheduler scheduler,
                             BuildHistoryManager historyManager, BuildTaskQueue taskQueue) {
        this.downloader = downloader;
        this.worldAdapter = worldAdapter;
        this.scheduler = scheduler;
        this.historyManager = historyManager;
        this.taskQueue = taskQueue;
    }

    public void startBuild(PlayerRef player, String url) {
        WorldRef world = worldAdapter.worldOf(player);

        CompletableFuture<List<VoxelBlock>> future;
        try {
            future = downloader.download(url);
        } catch (RuntimeException e) {
            // 地址非法等同步异常：终止本次构建（尚未放置任何方块，无需回滚）
            sendError(player, "模型数据获取失败: " + rootMessage(e));
            return;
        }

        sendInfo(player, "正在下载模型数据: " + url);
        future.whenComplete((blocks, error) -> {
            if (error != null) {
                scheduler.runOnServerThread(() -> sendError(player, "模型数据获取失败: " + rootMessage(error)));
                return;
            }
            // 玩家即使已离线，任务仍需入队并走完队列
            scheduler.runOnServerThread(() -> {
                ModelBuildJob job = ModelBuildJob.fromVoxelBlocks(player, world, blocks, worldAdapter);
                RegionSnapshot snapshot = new RegionSnapshot(world, job.getPlacements(), worldAdapter);
                historyManager.recordBuild(player, world, snapshot, job.getPlacements(), job);
                taskQueue.enqueue(job);
                sendInfo(player, "已获取 " + blocks.size() + " 个方块，开始分批构建...");
            });
        });
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
        worldAdapter.sendMessage(player, "[buildmodel] " + message, 0xFFFF5555);
    }

    private static String rootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }
}
