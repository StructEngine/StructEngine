package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.service.BuildHistoryManager;
import com.structengine.buildmodel.service.ModelDownloader;
import com.structengine.buildmodel.spi.IServerScheduler;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 远程 BUILD 调度：异步下载模型 → 主线程生成绝对坐标任务 → 压入原有 {@link BuildTaskQueue}。
 *
 * <p>不修改原有 {@code ModelBuildService}/{@code BuildTaskQueue} 的任何行为，
 * 只新增一条使用绝对原点的任务装配路径。</p>
 */
public class AbsoluteBuildService {

    private final ModelDownloader downloader;
    private final IWorldAdapter worldAdapter;
    private final IServerScheduler scheduler;
    private final BuildHistoryManager historyManager;
    private final BuildTaskQueue taskQueue;

    public AbsoluteBuildService(ModelDownloader downloader, IWorldAdapter worldAdapter,
                                IServerScheduler scheduler, BuildHistoryManager historyManager,
                                BuildTaskQueue taskQueue) {
        this.downloader = downloader;
        this.worldAdapter = worldAdapter;
        this.scheduler = scheduler;
        this.historyManager = historyManager;
        this.taskQueue = taskQueue;
    }

    /**
     * 必须在服务端主线程调用（由 {@link CoreCommandBridge} 保证）。
     */
    public void startBuild(String taskId, PlayerRef player, WorldRef world,
                           int originX, int originY, int originZ, String modelUrl,
                           Consumer<Throwable> onError) {
        CompletableFuture<List<VoxelBlock>> future;
        try {
            future = downloader.download(modelUrl);
        } catch (RuntimeException e) {
            fail(player, taskId, e, onError);
            return;
        }

        sendInfo(player, "任务 " + taskId + "：正在下载模型 " + modelUrl);
        future.whenComplete((blocks, error) -> {
            if (error != null) {
                runOnMain(() -> fail(player, taskId, error, onError), onError);
                return;
            }
            runOnMain(() -> assembleAndEnqueue(taskId, player, world, originX, originY, originZ, blocks, onError),
                    onError);
        });
    }

    private void assembleAndEnqueue(String taskId, PlayerRef player, WorldRef world,
                                    int originX, int originY, int originZ,
                                    List<VoxelBlock> blocks, Consumer<Throwable> onError) {
        try {
            AbsoluteModelBuildJob job = AbsoluteModelBuildJob.fromVoxelBlocks(
                    taskId, player, world, originX, originY, originZ, blocks, worldAdapter);
            if (job.getTotal() == 0) {
                throw new IllegalStateException("模型中没有可放置的方块");
            }

            RegionSnapshot snapshot = new RegionSnapshot(world, job.getPlacements(), worldAdapter);
            // associatedJob 传 null：回滚时仅按快照恢复，不影响已经在队列中的其他任务。
            historyManager.recordBuild(player, world, snapshot, job.getPlacements(), null);
            taskQueue.enqueue(job);
            sendInfo(player, "任务 " + taskId + "：已获取 " + job.getTotal()
                    + " 个方块，按绝对坐标 (" + originX + "," + originY + "," + originZ
                    + ") 开始分批构建...");
        } catch (Throwable e) {
            fail(player, taskId, e, onError);
        }
    }

    private void runOnMain(Runnable action, Consumer<Throwable> onError) {
        try {
            scheduler.runOnServerThread(action);
        } catch (Throwable e) {
            // 调度器不可用（如服务端正在关闭）时不能触碰世界/玩家 API，直接回传错误。
            onError.accept(e);
        }
    }

    private void fail(PlayerRef player, String taskId, Throwable error, Consumer<Throwable> onError) {
        String reason = rootMessage(error);
        if (worldAdapter.isOnline(player)) {
            worldAdapter.sendMessage(player, "[StructEngine] 任务 " + taskId + " 失败: " + reason, 0xFFFF5555);
        }
        onError.accept(error);
    }

    private void sendInfo(PlayerRef player, String message) {
        if (worldAdapter.isOnline(player)) {
            worldAdapter.sendMessage(player, message, 0xFFFFFF55);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }
}
