package com.structengine.buildmodel.support;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.spi.BlockStateSnapshot;
import com.structengine.buildmodel.spi.IServerScheduler;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 内存版平台实现：用于验证核心逻辑只依赖 SPI、不依赖任何 Minecraft API。
 */
public final class FakePlatform implements IWorldAdapter, IServerScheduler {

    public record FakePlayer(UUID id) implements PlayerRef {
    }

    public record FakeSnapshot(String value) implements BlockStateSnapshot {
    }

    private final WorldRef world = new WorldRef() {
    };
    private final PlayerRef player;
    private final BlockPosition playerBlockPos;
    private final Set<String> placeable;

    public final List<String> placed = new ArrayList<>();
    public final List<String> restored = new ArrayList<>();
    public final List<String> messages = new ArrayList<>();
    public final Deque<Runnable> tickHandlers = new ArrayDeque<>();

    public FakePlatform(UUID playerId, BlockPosition playerBlockPos, String... placeableIds) {
        this.player = new FakePlayer(playerId);
        this.playerBlockPos = playerBlockPos;
        this.placeable = new HashSet<>(List.of(placeableIds));
    }

    public PlayerRef player() {
        return player;
    }

    public WorldRef world() {
        return world;
    }

    @Override
    public WorldRef worldOf(PlayerRef player) {
        return world;
    }

    @Override
    public BlockPosition playerBlockPos(PlayerRef player) {
        return playerBlockPos;
    }

    @Override
    public boolean isPlaceable(String blockId) {
        return placeable.contains(blockId);
    }

    @Override
    public void placeBlock(WorldRef world, BlockPosition pos, String blockId) {
        placed.add(pos.x() + "," + pos.y() + "," + pos.z() + ":" + blockId);
    }

    @Override
    public BlockStateSnapshot snapshotBlock(WorldRef world, BlockPosition pos) {
        return new FakeSnapshot("before@" + pos.x() + "," + pos.y() + "," + pos.z());
    }

    @Override
    public void restoreBlock(WorldRef world, BlockPosition pos, BlockStateSnapshot snapshot) {
        restored.add(pos.x() + "," + pos.y() + "," + pos.z() + "<-" + ((FakeSnapshot) snapshot).value());
    }

    @Override
    public boolean isOnline(PlayerRef player) {
        return true;
    }

    @Override
    public void sendMessage(PlayerRef player, String message, int color) {
        messages.add(Integer.toHexString(color) + ":" + message);
    }

    @Override
    public void runOnServerThread(Runnable task) {
        task.run();
    }

    @Override
    public void onServerTick(Runnable task) {
        tickHandlers.add(task);
    }
}
