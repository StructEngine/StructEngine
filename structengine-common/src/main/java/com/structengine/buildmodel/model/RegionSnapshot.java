package com.structengine.buildmodel.model;

import com.structengine.buildmodel.spi.BlockStateSnapshot;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;

/**
 * 构建前的区域快照：记录每个目标坐标的原始方块状态，用于回滚。
 */
public class RegionSnapshot {
    private final BlockPosition[] positions;
    private final BlockStateSnapshot[] originalStates;
    private final IWorldAdapter worldAdapter;
    private int restoreIndex;

    public RegionSnapshot(WorldRef world, List<BlockPlacement> placements, IWorldAdapter worldAdapter) {
        int size = placements.size();
        this.positions = new BlockPosition[size];
        this.originalStates = new BlockStateSnapshot[size];
        this.worldAdapter = worldAdapter;
        for (int i = 0; i < size; i++) {
            BlockPosition pos = placements.get(i).getPos();
            this.positions[i] = pos;
            this.originalStates[i] = worldAdapter.snapshotBlock(world, pos);
        }
    }

    public boolean restoreNext(WorldRef world) {
        if (restoreIndex >= positions.length) {
            return false;
        }
        worldAdapter.restoreBlock(world, positions[restoreIndex], originalStates[restoreIndex]);
        restoreIndex++;
        return true;
    }

    public boolean isFullyRestored() {
        return restoreIndex >= positions.length;
    }

    public void reset() {
        restoreIndex = 0;
    }

    public int getTotal() {
        return positions.length;
    }
}
