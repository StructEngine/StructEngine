package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.model.VoxelBlock;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个模型的构建任务：坐标映射、方块解析与分批放置。
 * <p>坐标映射规则保持不变：相对玩家方块坐标偏移 {@code x + 1}、{@code y + 0}、{@code z + 1}。
 */
public class ModelBuildJob implements BuildJob {
    private final PlayerRef player;
    private final WorldRef world;
    private final List<BlockPlacement> placements;
    private final IWorldAdapter worldAdapter;
    private int index;

    private ModelBuildJob(PlayerRef player, WorldRef world, List<BlockPlacement> placements, IWorldAdapter worldAdapter) {
        this.player = player;
        this.world = world;
        this.placements = placements;
        this.worldAdapter = worldAdapter;
    }

    public static ModelBuildJob fromVoxelBlocks(PlayerRef player, WorldRef world, List<VoxelBlock> blocks,
                                                IWorldAdapter worldAdapter) {
        BlockPosition playerPos = worldAdapter.playerBlockPos(player);
        List<BlockPlacement> list = new ArrayList<>(blocks.size());
        for (VoxelBlock voxel : blocks) {
            BlockPosition target = new BlockPosition(
                    playerPos.x() + 1 + voxel.getX(),
                    playerPos.y() + voxel.getY(),
                    playerPos.z() + 1 + voxel.getZ()
            );
            if (worldAdapter.isPlaceable(voxel.getId())) {
                list.add(new BlockPlacement(target, voxel.getId()));
            }
        }
        return new ModelBuildJob(player, world, list, worldAdapter);
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
        return true;
    }

    @Override
    public void notifyComplete() {
        if (!worldAdapter.isOnline(player)) {
            return;
        }
        worldAdapter.sendMessage(player, "模型构建完毕！共 " + placements.size() + " 个方块。", 0xFF55FF55);
    }

    @Override
    public void notifyAborted(String reason) {
        if (!worldAdapter.isOnline(player)) {
            return;
        }
        worldAdapter.sendMessage(player, "[buildmodel] 模型构建中断: " + reason, 0xFFFF5555);
    }
}
