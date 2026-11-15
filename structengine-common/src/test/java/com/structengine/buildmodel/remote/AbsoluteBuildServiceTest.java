package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.service.BuildHistoryManager;
import com.structengine.buildmodel.service.ModelDownloader;
import com.structengine.buildmodel.support.FakePlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbsoluteBuildServiceTest {

    private FakePlatform platform;
    private BuildTaskQueue queue;
    private BuildHistoryManager history;
    private RecordingScheduler scheduler;

    @BeforeEach
    void setUp() {
        platform = new FakePlatform(UUID.randomUUID(), new BlockPosition(0, 0, 0),
                "minecraft:stone", "minecraft:glass");
        queue = new BuildTaskQueue();
        history = new BuildHistoryManager(platform, queue);
        scheduler = new RecordingScheduler();
    }

    @Test
    void downloadsThenBuildsAtAbsoluteOriginOnMainThread() {
        StubDownloader downloader = new StubDownloader(CompletableFuture.completedFuture(List.of(
                new VoxelBlock("minecraft:stone", 0, 1, 0),
                new VoxelBlock("minecraft:glass", 2, 0, 3),
                new VoxelBlock("minecraft:air", 0, 0, 0)
        )));
        AbsoluteBuildService service = new AbsoluteBuildService(downloader, platform, scheduler, history, queue);
        List<Throwable> errors = new ArrayList<>();

        service.startBuild("task-1", platform.player(), platform.world(),
                100, 64, -50, "http://platform/models/task-1.json", errors::add);

        assertTrue(errors.isEmpty());
        assertEquals(1, scheduler.pending(), "任务装配必须投递到主线程");
        assertTrue(queue.isEmpty());

        scheduler.runAll();
        assertEquals(1, queue.pendingJobs(), "装配完成后应压入原有任务队列");
        assertEquals(2, platform.messages.size());

        assertEquals(2, queue.tick(1000));
        assertEquals(List.of("100,65,-50:minecraft:stone", "102,64,-47:minecraft:glass"), platform.placed);
    }

    @Test
    void recordsSnapshotSoExistingRollbackStillWorks() {
        StubDownloader downloader = new StubDownloader(CompletableFuture.completedFuture(List.of(
                new VoxelBlock("minecraft:stone", 0, 0, 0)
        )));
        AbsoluteBuildService service = new AbsoluteBuildService(downloader, platform, scheduler, history, queue);
        service.startBuild("task-2", platform.player(), platform.world(), 5, 70, 5, "http://x/m.json", e -> { });
        scheduler.runAll();
        queue.tick(1000);

        assertEquals(1, history.undoCount(platform.player()));
        history.rollback(platform.player());
        queue.tick(1000);

        assertTrue(platform.restored.contains("5,70,5<-before@5,70,5"));
    }

    @Test
    void downloadFailureIsReportedWithoutEnqueue() {
        StubDownloader downloader = new StubDownloader(
                CompletableFuture.failedFuture(new RuntimeException("网络断开")));
        AbsoluteBuildService service = new AbsoluteBuildService(downloader, platform, scheduler, history, queue);
        List<Throwable> errors = new ArrayList<>();

        service.startBuild("task-3", platform.player(), platform.world(), 0, 64, 0, "http://x/m.json", errors::add);
        scheduler.runAll();

        assertEquals(1, errors.size());
        assertTrue(queue.isEmpty());
        assertTrue(platform.messages.stream().anyMatch(message -> message.contains("失败")));
    }

    @Test
    void allUnplaceableBlocksIsAnError() {
        StubDownloader downloader = new StubDownloader(CompletableFuture.completedFuture(List.of(
                new VoxelBlock("minecraft:air", 0, 0, 0)
        )));
        AbsoluteBuildService service = new AbsoluteBuildService(downloader, platform, scheduler, history, queue);
        List<Throwable> errors = new ArrayList<>();

        service.startBuild("task-4", platform.player(), platform.world(), 0, 64, 0, "http://x/m.json", errors::add);
        scheduler.runAll();

        assertEquals(1, errors.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    void invalidUrlFailsSynchronously() {
        StubDownloader downloader = new StubDownloader(CompletableFuture.completedFuture(List.of()));
        downloader.throwOnDownload = true;
        AbsoluteBuildService service = new AbsoluteBuildService(downloader, platform, scheduler, history, queue);
        List<Throwable> errors = new ArrayList<>();

        service.startBuild("task-5", platform.player(), platform.world(), 0, 64, 0, "bad-url", errors::add);

        assertEquals(1, errors.size());
        assertEquals(0, scheduler.pending());
    }

    private static final class StubDownloader extends ModelDownloader {
        private final CompletableFuture<List<VoxelBlock>> result;
        private boolean throwOnDownload;

        private StubDownloader(CompletableFuture<List<VoxelBlock>> result) {
            this.result = result;
        }

        @Override
        public CompletableFuture<List<VoxelBlock>> download(String url) {
            if (throwOnDownload) {
                throw new IllegalArgumentException("bad url");
            }
            return result;
        }
    }
}
