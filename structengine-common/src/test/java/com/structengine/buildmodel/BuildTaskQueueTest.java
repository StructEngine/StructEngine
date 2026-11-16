package com.structengine.buildmodel;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.queue.ModelBuildJob;
import com.structengine.buildmodel.support.FakePlatform;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildTaskQueueTest {

    private static ModelBuildJob job(FakePlatform platform, int blocks) {
        return ModelBuildJob.fromVoxelBlocks(platform.player(), platform.world(),
                List.of(new VoxelBlock("minecraft:stone", 0, 0, 0),
                        new VoxelBlock("minecraft:stone", blocks, 0, 0),
                        new VoxelBlock("minecraft:stone", blocks + 1, 0, 0)), platform);
    }

    @Test
    void tickRespectsMaxBlocksPerTickAndKeepsRemainingWork() {
        FakePlatform platform = new FakePlatform(UUID.randomUUID(), new BlockPosition(0, 0, 0), "minecraft:stone");
        BuildTaskQueue queue = new BuildTaskQueue();
        queue.enqueue(job(platform, 1));

        assertEquals(2, queue.tick(2));
        assertEquals(2, platform.placed.size());
        assertEquals(1, queue.pendingJobs());

        assertEquals(1, queue.tick(1000));
        assertTrue(queue.isEmpty());
        assertEquals(List.of("ff55ff55:模型构建完毕！共 3 个方块。"), platform.messages);
    }

    @Test
    void interleavesDifferentPlayersWithinTickBudget() {
        FakePlatform first = new FakePlatform(UUID.randomUUID(), new BlockPosition(0, 0, 0), "minecraft:stone");
        FakePlatform second = new FakePlatform(UUID.randomUUID(), new BlockPosition(0, 0, 0), "minecraft:stone");
        BuildTaskQueue queue = new BuildTaskQueue();
        queue.enqueue(job(first, 1));
        queue.enqueue(job(second, 1));

        assertEquals(2, queue.tick(2));
        assertEquals(1, first.placed.size());
        assertEquals(1, second.placed.size());

        assertEquals(4, queue.tick(1000));
        assertEquals(3, first.placed.size());
        assertEquals(3, second.placed.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    void processesQueuedJobsInFifoOrder() {
        FakePlatform platform = new FakePlatform(UUID.randomUUID(), new BlockPosition(0, 0, 0), "minecraft:stone");
        BuildTaskQueue queue = new BuildTaskQueue();
        queue.enqueue(job(platform, 1));
        queue.enqueue(job(platform, 5));

        assertEquals(6, queue.tick(1000));
        assertTrue(queue.isEmpty());
        assertEquals(2, platform.messages.size());
        assertEquals("3,0,1:minecraft:stone", platform.placed.get(2));
    }
}
