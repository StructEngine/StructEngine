package com.structengine.buildmodel.remote;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.queue.BuildJob;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 绝对坐标构建任务：把模型相对偏移直接叠加到 Platform 下发的 {@code (x,y,z)} 原点。
 *
 * <p>与原有 {@code ModelBuildJob} 的区别仅在于坐标来源：不读取玩家位置、不再计算高度。
 * 分批放置、快照记录、回滚/恢复仍完全复用原有队列与历史机制。</p>
 */
public class AbsoluteModelBuildJob implements BuildJob {

    private final String taskId;
    private final PlayerRef player;
    private final WorldRef world;
    private final List<BlockPlacement> placements;
    private final IWorldAdapter worldAdapter;
    private final Consumer<RemoteBuildStatus> onStatus;
    private int index;

    private AbsoluteModelBuildJob(String taskId, PlayerRef player, WorldRef world,
                                  List<BlockPlacement> placements, IWorldAdapter worldAdapter,
                                  Consumer<RemoteBuildStatus> onStatus) {
        this.taskId = taskId;
        this.player = player;
        this.world = world;
        this.placements = placements;
        this.worldAdapter = worldAdapter;
        this.onStatus = onStatus == null ? status -> { } : onStatus;
    }

    public static AbsoluteModelBuildJob fromVoxelBlocks(String taskId, PlayerRef player, WorldRef world,
                                                        int originX, int originY, int originZ,
                                                        List<VoxelBlock> blocks, IWorldAdapter worldAdapter) {
        return fromVoxelBlocks(taskId, player, world, originX, originY, originZ, blocks, worldAdapter,
                status -> { });
    }

    public static AbsoluteModelBuildJob fromVoxelBlocks(String taskId, PlayerRef player, WorldRef world,
                                                        int originX, int originY, int originZ,
                                                        List<VoxelBlock> blocks, IWorldAdapter worldAdapter,
                                                        Consumer<RemoteBuildStatus> onStatus) {
        List<BlockPlacement> list = new ArrayList<>(blocks.size());
        for (VoxelBlock voxel : blocks) {
            BlockPosition target = new BlockPosition(
                    originX + voxel.getX(),
                    originY + voxel.getY(),
                    originZ + voxel.getZ()
            );
            if (worldAdapter.isPlaceable(voxel.getId())) {
                list.add(new BlockPlacement(target, voxel.getId()));
            }
        }
        return new AbsoluteModelBuildJob(taskId, player, world, list, worldAdapter, onStatus);
    }

    public String getTaskId() {
        return taskId;
    }

    public PlayerRef getPlayer() {
        return player;
    }

    public List<BlockPlacement> getPlacements() {
        return placements;
    }

    public int getTotal() {
        return placements.size();
    }

    @Override
    public java.util.UUID playerId() {
        return player.id();
    }

    @Override
    public boolean isDone() {
        return index >= placements.size();
    }

    @Override
    public boolean processNext() {
        if (isDone()) {
            return false;
        }
        BlockPlacement placement = placements.get(index++);
        worldAdapter.placeBlock(world, placement.getPos(), placement.getBlockId());
        if (index % 100 == 0 || index >= placements.size()) {
            onStatus.accept(RemoteBuildStatus.of("BUILDING", index, placements.size(), "正在分批放置"));
        }
        return true;
    }

    @Override
    public void notifyComplete() {
        onStatus.accept(RemoteBuildStatus.of("COMPLETED", placements.size(), placements.size(), "构建完成"));
        if (!worldAdapter.isOnline(player)) {
            return;
        }
        worldAdapter.sendMessage(player,
                "远程任务 " + taskId + " 构建完毕！共 " + placements.size() + " 个方块。", 0xFF55FF55);
    }

    @Override
    public void notifyAborted(String reason) {
        onStatus.accept(RemoteBuildStatus.of("FAILED", index, placements.size(), reason));
        if (!worldAdapter.isOnline(player)) {
            return;
        }
        worldAdapter.sendMessage(player,
                "[StructEngine] 远程任务 " + taskId + " 构建中断: " + reason, 0xFFFF5555);
    }
}
