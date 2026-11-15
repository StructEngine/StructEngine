package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.function.Consumer;

/**
 * 把网络层指令切换到游戏主线程，再调用核心 SPI / 原有任务队列。
 *
 * <p>这里绝不直接读写方块：高度查询与任务装配都通过 {@link com.structengine.buildmodel.spi.IServerScheduler}
 * 投递，遵守 Minecraft 服务端主线程约束。</p>
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
        try {
            core.scheduler().runOnServerThread(() -> {
                try {
                    WorldRef world = context.defaultWorld();
                    int y = core.worldAdapter().highestBlockYAt(world, x, z);
                    onY.accept(y);
                } catch (Throwable e) {
                    onError.accept(e);
                }
            });
        } catch (Throwable e) {
            onError.accept(e);
        }
    }

    @Override
    public void enqueueBuild(String taskId, String playerName, int x, int y, int z,
                             String modelUrl, Consumer<Throwable> onError) {
        try {
            core.scheduler().runOnServerThread(() -> {
                try {
                    PlayerRef player = context.resolvePlayer(playerName);
                    WorldRef world = context.defaultWorld();
                    buildService.startBuild(taskId, player, world, x, y, z, modelUrl, onError);
                } catch (Throwable e) {
                    onError.accept(e);
                }
            });
        } catch (Throwable e) {
            onError.accept(e);
        }
    }
}
