package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.BlockPlacement;
import com.structengine.buildmodel.model.VoxelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ModelBuildJob implements BuildJob {
    private final ServerPlayerEntity player;
    private final World world;
    private final List<BlockPlacement> placements;
    private int index;

    private ModelBuildJob(ServerPlayerEntity player, World world, List<BlockPlacement> placements) {
        this.player = player;
        this.world = world;
        this.placements = placements;
    }

    public static ModelBuildJob fromVoxelBlocks(ServerPlayerEntity player, World world, List<VoxelBlock> blocks) {
        BlockPos playerPos = player.getBlockPos();
        List<BlockPlacement> list = new ArrayList<>(blocks.size());
        for (VoxelBlock voxel : blocks) {
            BlockPos target = new BlockPos(
                    playerPos.getX() + 1 + voxel.getX(),
                    playerPos.getY() + voxel.getY(),
                    playerPos.getZ() + 1 + voxel.getZ()
            );
            BlockState state = resolveBlockState(voxel.getId());
            if (state != null) {
                list.add(new BlockPlacement(target, state));
            }
        }
        return new ModelBuildJob(player, world, list);
    }

    private static BlockState resolveBlockState(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            return null;
        }
        Block block = Registries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            return null;
        }
        return block.getDefaultState();
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }

    public List<BlockPlacement> getPlacements() {
        return placements;
    }

    public int getTotal() {
        return placements.size();
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
        world.setBlockState(placement.getPos(), placement.getState(), 3);
        return true;
    }

    @Override
    public void notifyComplete() {
        if (!player.isDisconnected()) {
            player.sendMessage(Text.literal("模型构建完毕！共 " + placements.size() + " 个方块。").withColor(0xFF55FF55));
        }
    }

    @Override
    public void notifyAborted(String reason) {
        if (!player.isDisconnected()) {
            player.sendMessage(Text.literal("[buildmodel] 模型构建中断: " + reason).withColor(0xFFFF5555));
        }
    }
}
