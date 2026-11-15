package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.service.BuildHistoryManager;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;

public class RedoJob implements BuildJob {
    private final PlayerRef player;
    private final WorldRef world;
    private final List<BlockPlacement> placements;
    private final BuildHistoryManager historyManager;
    private final IWorldAdapter worldAdapter;
    private int index;

    public RedoJob(PlayerRef player, WorldRef world, List<BlockPlacement> placements,
                   BuildHistoryManager historyManager, IWorldAdapter worldAdapter) {
        this.player = player;
        this.world = world;
        this.placements = placements;
        this.historyManager = historyManager;
        this.worldAdapter = worldAdapter;
        this.index = 0;
    }

    @Override
    public boolean isDone() {
        return index >= placements.size();
    }

    @Override
    public boolean processNext() {
        if (isDone()) {
            return false;
        }
        BlockPlacement placement = placements.get(index++);
        worldAdapter.placeBlock(world, placement.getPos(), placement.getBlockId());
        return true;
    }

    @Override
    public void notifyComplete() {
        historyManager.onRestoreComplete(player.id());
        if (worldAdapter.isOnline(player)) {
            worldAdapter.sendMessage(player, "恢复完成！已重新放置 " + placements.size() + " 个方块。", 0xFF55FF55);
        }
    }

    @Override
    public void notifyAborted(String reason) {
        historyManager.onRestoreComplete(player.id());
        if (worldAdapter.isOnline(player)) {
            worldAdapter.sendMessage(player, "[redo] 恢复中断: " + reason, 0xFFFF5555);
        }
    }
}
