package com.structengine.buildmodel.model;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class RegionSnapshot {
    private final BlockPos[] positions;
    private final BlockState[] originalStates;
    private int restoreIndex;

    public RegionSnapshot(World world, List<BlockPlacement> placements) {
        int size = placements.size();
        this.positions = new BlockPos[size];
        this.originalStates = new BlockState[size];
        for (int i = 0; i < size; i++) {
            BlockPos pos = placements.get(i).getPos();
            this.positions[i] = pos;
            this.originalStates[i] = world.getBlockState(pos);
        }
    }

    public boolean restoreNext(World world) {
        if (restoreIndex >= positions.length) {
            return false;
        }
        world.setBlockState(positions[restoreIndex], originalStates[restoreIndex], 3);
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