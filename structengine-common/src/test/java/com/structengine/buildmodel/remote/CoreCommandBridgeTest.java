package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import com.structengine.buildmodel.support.FakePlatform;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreCommandBridgeTest {

    private final FakePlatform platform = new FakePlatform(UUID.randomUUID(),
            new BlockPosition(0, 0, 0), "minecraft:stone");
    private final RecordingScheduler scheduler = new RecordingScheduler();
    private final StructEngineCore core = new StructEngineCore(platform, scheduler);
    private final CoreCommandBridge bridge = new CoreCommandBridge(core, new GameServerContext() {
        @Override
        public PlayerRef resolvePlayer(String username) {
            return platform.player();
        }

        @Override
        public java.util.List<RemotePlayerInfo> onlinePlayers() {
            return java.util.List.of();
        }

        @Override
        public WorldRef defaultWorld() {
            return platform.world();
        }
    });

    @Test
    void queryIsHoppedToServerThread() {
        platform.topY = 77;
        AtomicInteger y = new AtomicInteger(-1);
        List<Throwable> errors = new ArrayList<>();

        bridge.queryHighestY("t-1", 10, -10, y::set, errors::add);

        assertEquals(-1, y.get(), "网络线程不能直接触碰世界");
        assertEquals(1, scheduler.pending());
        scheduler.runAll();
        assertEquals(77, y.get());
        assertTrue(errors.isEmpty());
    }

    @Test
    void buildIsHoppedToServerThreadAndReportsInvalidUrl() {
        List<Throwable> errors = new ArrayList<>();

        bridge.enqueueBuild("t-2", "Steve", 1, 64, 2, "bad-url", errors::add);

        assertEquals(0, errors.size());
        assertEquals(1, scheduler.pending());
        scheduler.runAll();
        assertEquals(1, errors.size(), "无效模型地址应通过回调上报");
    }

    @Test
    void schedulerFailureIsReported() {
        com.structengine.buildmodel.spi.IServerScheduler failing = new com.structengine.buildmodel.spi.IServerScheduler() {
            @Override
            public void runOnServerThread(Runnable task) {
                throw new IllegalStateException("server not ready");
            }

            @Override
            public void onServerTick(Runnable task) {
            }
        };
        StructEngineCore failingCore = new StructEngineCore(platform, failing);
        CoreCommandBridge failingBridge = new CoreCommandBridge(failingCore, new GameServerContext() {
            @Override
            public PlayerRef resolvePlayer(String username) {
                return platform.player();
            }

            @Override
            public java.util.List<RemotePlayerInfo> onlinePlayers() {
                return java.util.List.of();
            }

            @Override
            public WorldRef defaultWorld() {
                return platform.world();
            }
        });
        List<Throwable> errors = new ArrayList<>();

        failingBridge.queryHighestY("t-3", 0, 0, y -> { }, errors::add);

        assertEquals(1, errors.size());
    }
}
