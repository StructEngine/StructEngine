package com.structengine.buildmodel;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.queue.ModelBuildJob;
import com.structengine.buildmodel.support.FakePlatform;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelBuildJobTest {

    private static FakePlatform platform() {
        return new FakePlatform(UUID.randomUUID(), new BlockPosition(10, 64, -5),
                "minecraft:stone", "minecraft:glass");
    }

    @Test
    void mapsCoordinatesByPlayerOffsetAndSkipsUnknownBlocks() {
        FakePlatform platform = platform();
        List<VoxelBlock> blocks = List.of(
                new VoxelBlock("minecraft:stone", 0, 0, 0),
                new VoxelBlock("minecraft:glass", 1, 2, 3),
                new VoxelBlock("minecraft:air", 0, 1, 0),
                new VoxelBlock("bad id", 0, 0, 0));

        ModelBuildJob job = ModelBuildJob.fromVoxelBlocks(platform.player(), platform.world(), blocks, platform);

        assertEquals(2, job.getTotal());
        assertEquals(new BlockPosition(11, 64, -4), job.getPlacements().get(0).getPos());
        assertEquals("minecraft:stone", job.getPlacements().get(0).getBlockId());
        assertEquals(new BlockPosition(12, 66, -1), job.getPlacements().get(1).getPos());
    }

    @Test
    void placesBlocksInOrderAndReportsCompletion() {
        FakePlatform platform = platform();
        ModelBuildJob job = ModelBuildJob.fromVoxelBlocks(platform.player(), platform.world(),
                List.of(new VoxelBlock("minecraft:stone", 0, 0, 0),
                        new VoxelBlock("minecraft:glass", 1, 2, 3)), platform);

        while (!job.isDone()) {
            assertTrue(job.processNext());
        }
        assertFalse(job.processNext());

        assertEquals(List.of("11,64,-4:minecraft:stone", "12,66,-1:minecraft:glass"), platform.placed);

        job.notifyComplete();
        assertEquals(List.of("ff55ff55:模型构建完毕！共 2 个方块。"), platform.messages);
    }

    @Test
    void snapshotRestoresOriginalStatesInOrder() {
        FakePlatform platform = platform();
        ModelBuildJob job = ModelBuildJob.fromVoxelBlocks(platform.player(), platform.world(),
                List.of(new VoxelBlock("minecraft:stone", 0, 0, 0),
                        new VoxelBlock("minecraft:glass", 1, 2, 3)), platform);

        RegionSnapshot snapshot = new RegionSnapshot(platform.world(), job.getPlacements(), platform);
        assertEquals(2, snapshot.getTotal());

        assertTrue(snapshot.restoreNext(platform.world()));
        assertTrue(snapshot.restoreNext(platform.world()));
        assertTrue(snapshot.isFullyRestored());
        assertFalse(snapshot.restoreNext(platform.world()));

        assertEquals(List.of("11,64,-4<-before@11,64,-4", "12,66,-1<-before@12,66,-1"), platform.restored);
    }
}
