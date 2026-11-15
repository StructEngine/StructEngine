package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.service.BuildHistoryManager;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

public class RollbackJob implements BuildJob {
    private final PlayerRef player;
    private final WorldRef world;
    private final RegionSnapshot snapshot;
    private final BuildHistoryManager historyManager;
    private final IWorldAdapter worldAdapter;

    public RollbackJob(PlayerRef player, WorldRef world, RegionSnapshot snapshot,
                       BuildHistoryManager historyManager, IWorldAdapter worldAdapter) {
        this.player = player;
        this.world = world;
        this.snapshot = snapshot;
        this.historyManager = historyManager;
        this.worldAdapter = worldAdapter;
        this.snapshot.reset();
    }

    @Override
    public boolean isDone() {
        return snapshot.isFullyRestored();
    }

    @Override
    public boolean processNext() {
        return snapshot.restoreNext(world);
    }

    @Override
    public void notifyComplete() {
        historyManager.onRestoreComplete(player.id());
        if (worldAdapter.isOnline(player)) {
            worldAdapter.sendMessage(player, "回滚完成！已恢复 " + snapshot.getTotal() + " 个方块。", 0xFF55FF55);
        }
    }

    @Override
    public void notifyAborted(String reason) {
        historyManager.onRestoreComplete(player.id());
        if (worldAdapter.isOnline(player)) {
            worldAdapter.sendMessage(player, "[rollback] 回滚中断: " + reason, 0xFFFF5555);
        }
    }
}
