package com.structengine.neoforge.adapter;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.spi.BlockStateSnapshot;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge 世界适配器：与 Fabric 实现一一对应，仅替换为 Mojang 映射 API。
 */
public class NeoForgeWorldAdapter implements IWorldAdapter {

    @Override
    public WorldRef worldOf(PlayerRef player) {
        return new NeoForgeWorldRef(player(player).level());
    }

    @Override
    public BlockPosition playerBlockPos(PlayerRef player) {
        BlockPos pos = player(player).blockPosition();
        return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean isPlaceable(String blockId) {
        return resolveBlockState(blockId) != null;
    }

    @Override
    public void placeBlock(WorldRef world, BlockPosition pos, String blockId) {
        BlockState state = resolveBlockState(blockId);
        if (state == null) {
            return;
        }
        world(world).setBlock(toBlockPos(pos), state, 3);
    }

    @Override
    public BlockStateSnapshot snapshotBlock(WorldRef world, BlockPosition pos) {
        return new NeoForgeBlockStateSnapshot(world(world).getBlockState(toBlockPos(pos)));
    }

    @Override
    public void restoreBlock(WorldRef world, BlockPosition pos, BlockStateSnapshot snapshot) {
        world(world).setBlock(toBlockPos(pos), ((NeoForgeBlockStateSnapshot) snapshot).handle(), 3);
    }

    @Override
    public boolean isOnline(PlayerRef player) {
        return !player(player).hasDisconnected();
    }

    @Override
    public void sendMessage(PlayerRef player, String message, int color) {
        player(player).sendSystemMessage(Component.literal(message).withColor(color));
    }

    private static BlockState resolveBlockState(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        Identifier id = Identifier.tryParse(blockId);
        if (id == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        return block.defaultBlockState();
    }

    private static ServerPlayer player(PlayerRef player) {
        return ((NeoForgePlayerRef) player).handle();
    }

    private static Level world(WorldRef world) {
        return ((NeoForgeWorldRef) world).handle();
    }

    private static BlockPos toBlockPos(BlockPosition pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
}
