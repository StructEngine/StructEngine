package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.List;
import java.util.UUID;

public class BuildRecord {
    private final String historyId;
    private final String taskId;
    private final String modelName;
    private final long createdAt;
    private final WorldRef world;
    private final RegionSnapshot beforeSnapshot;
    private final List<BlockPlacement> placements;
    private final ModelBuildJob associatedJob;

    public BuildRecord(WorldRef world, RegionSnapshot snapshot, List<BlockPlacement> placements, ModelBuildJob job) {
        this(UUID.randomUUID().toString(), null, null, System.currentTimeMillis(),
                world, snapshot, placements, job);
    }

    public BuildRecord(String historyId, String taskId, String modelName, long createdAt,
                       WorldRef world, RegionSnapshot snapshot, List<BlockPlacement> placements,
                       ModelBuildJob job) {
        this.historyId = historyId;
        this.taskId = taskId;
        this.modelName = modelName;
        this.createdAt = createdAt;
        this.world = world;
        this.beforeSnapshot = snapshot;
        this.placements = placements;
        this.associatedJob = job;
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getModelName() {
        return modelName;
    }

    public long getCreatedAt() {
        return createdAt;
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
