package com.realcraft.buildmodel.queue;

import com.realcraft.buildmodel.model.BlockPlacement;
import com.realcraft.buildmodel.model.RegionSnapshot;
import net.minecraft.world.World;

import java.util.List;

public class BuildRecord {
    private final World world;
    private final RegionSnapshot beforeSnapshot;
    private final List<BlockPlacement> placements;
    private final ModelBuildJob associatedJob;

    public BuildRecord(World world, RegionSnapshot snapshot, List<BlockPlacement> placements, ModelBuildJob job) {
        this.world = world;
        this.beforeSnapshot = snapshot;
        this.placements = placements;
        this.associatedJob = job;
    }

    public World getWorld() {
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