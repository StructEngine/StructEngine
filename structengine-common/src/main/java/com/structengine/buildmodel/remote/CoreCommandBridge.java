package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import com.structengine.buildmodel.service.BuildHistoryView;

import java.util.List;
import java.util.function.Consumer;

/**
 * 把网络层指令切换到游戏主线程，再调用核心 SPI / 原有任务队列。
 */
public final class CoreCommandBridge implements RemoteCommandBridge {

    private final StructEngineCore core;
    private final GameServerContext context;
    private final AbsoluteBuildService buildService;

    public CoreCommandBridge(StructEngineCore core, GameServerContext context) {
        this.core = core;
        this.context = context;
        this.buildService = new AbsoluteBuildService(
                core.downloader(),
                core.worldAdapter(),
                core.scheduler(),
                core.historyManager(),
                core.taskQueue()
        );
    }

    @Override
    public void queryHighestY(String taskId, int x, int z, Consumer<Integer> onY, Consumer<Throwable> onError) {
        runOnMain(() -> {
            WorldRef world = context.defaultWorld();
            return core.worldAdapter().highestBlockYAt(world, x, z);
        }, onY, onError);
    }

    @Override
    public void players(Consumer<List<RemotePlayerInfo>> onPlayers, Consumer<Throwable> onError) {
        runOnMain(context::onlinePlayers, onPlayers, onError);
    }

    @Override
    public void enqueueBuild(String taskId, String playerUuid, String playerName,
                             int x, int y, int z, String modelUrl, String modelName, int rotation,
                             Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
        runOnMain(() -> {
            PlayerRef player = context.resolvePlayer(playerName, playerUuid);
            WorldRef world = context.defaultWorld();
            buildService.startBuild(taskId, player, world, x, y, z, modelUrl, modelName, rotation,
                    onStatus, onError);
            return null;
        }, ignored -> { }, onError);
    }

    @Override
    public void history(String requestId, String playerUuid, String playerName,
                        Consumer<List<RemoteHistoryEntry>> onHistory, Consumer<Throwable> onError) {
        runOnMain(() -> {
            PlayerRef player = context.resolvePlayer(playerName, playerUuid);
            return core.historyManager().history(player).stream()
                    .map(CoreCommandBridge::toRemoteHistory)
                    .toList();
        }, onHistory, onError);
    }

    @Override
    public void rollback(String requestId, String playerUuid, String playerName, String historyId,
                         Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
        runOnMain(() -> {
            PlayerRef player = context.resolvePlayer(playerName, playerUuid);
            boolean accepted = core.historyManager().rollback(player, historyId);
            if (!accepted) {
                throw new IllegalStateException("回滚未被接受");
            }
            return RemoteBuildStatus.of("ROLLBACKING", 0, 0, "已进入回滚队列");
        }, onStatus, onError);
    }

    @Override
    public void redo(String requestId, String playerUuid, String playerName, String historyId,
                     Consumer<RemoteBuildStatus> onStatus, Consumer<Throwable> onError) {
        runOnMain(() -> {
            PlayerRef player = context.resolvePlayer(playerName, playerUuid);
            boolean accepted = core.historyManager().redo(player, historyId);
            if (!accepted) {
                throw new IllegalStateException("恢复未被接受");
            }
            return RemoteBuildStatus.of("REDOING", 0, 0, "已进入恢复队列");
        }, onStatus, onError);
    }

    private static RemoteHistoryEntry toRemoteHistory(BuildHistoryView view) {
        return new RemoteHistoryEntry(view.historyId(), view.taskId(), view.modelName(),
                view.createdAt(), view.blockCount(), view.canRollback(), view.canRedo());
    }

    private <T> void runOnMain(ThrowingSupplier<T> action, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        try {
            core.scheduler().runOnServerThread(() -> {
                try {
                    onSuccess.accept(action.get());
                } catch (Throwable e) {
                    onError.accept(e);
                }
            });
        } catch (Throwable e) {
            onError.accept(e);
        }
    }

    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
