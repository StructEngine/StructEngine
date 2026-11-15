package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;

public class BuildRecord {
    private final WorldRef world;
    private final RegionSnapshot beforeSnapshot;
    private final List<BlockPlacement> placements;
    private final ModelBuildJob associatedJob;

    public BuildRecord(WorldRef world, RegionSnapshot snapshot, List<BlockPlacement> placements, ModelBuildJob job) {
        this.world = world;
        this.beforeSnapshot = snapshot;
        this.placements = placements;
        this.associatedJob = job;
    }

    public WorldRef getWorld() {
        return world;
    }

    public RegionSnapshot getSnapshot() {
        return beforeSnapshot;
    }

    public List<BlockPlacement> getPlacements() {
        return placements;
    }

    public ModelBuildJob getAssociatedJob() {
        return associatedJob;
    }
}
