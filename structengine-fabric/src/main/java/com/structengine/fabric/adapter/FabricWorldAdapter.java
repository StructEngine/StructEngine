package com.structengine.fabric.adapter;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.spi.BlockStateSnapshot;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Fabric 世界适配器：承载原业务代码中直接调用的 MC API。
 */
public class FabricWorldAdapter implements IWorldAdapter {

    @Override
    public WorldRef worldOf(PlayerRef player) {
        return new FabricWorldRef(player(player).getEntityWorld());
    }

    @Override
    public BlockPosition playerBlockPos(PlayerRef player) {
        BlockPos pos = player(player).getBlockPos();
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
        world(world).setBlockState(toBlockPos(pos), state, 3);
    }

    @Override
    public BlockStateSnapshot snapshotBlock(WorldRef world, BlockPosition pos) {
        return new FabricBlockStateSnapshot(world(world).getBlockState(toBlockPos(pos)));
    }

    @Override
    public void restoreBlock(WorldRef world, BlockPosition pos, BlockStateSnapshot snapshot) {
        world(world).setBlockState(toBlockPos(pos), ((FabricBlockStateSnapshot) snapshot).handle(), 3);
    }

    @Override
    public int highestBlockYAt(WorldRef world, int x, int z) {
        World level = world(world);
        // 主线程同步加载区块，避免未加载区块返回世界底部。
        WorldChunk chunk = level.getChunk(x >> 4, z >> 4);
        int top = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x & 15, z & 15) - 1;
        return Math.max(level.getBottomY(), top);
    }

    @Override
    public boolean isOnline(PlayerRef player) {
        return !player(player).isDisconnected();
    }

    @Override
    public void sendMessage(PlayerRef player, String message, int color) {
        player(player).sendMessage(Text.literal(message).withColor(color));
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

    private static net.minecraft.server.network.ServerPlayerEntity player(PlayerRef player) {
        return ((FabricPlayerRef) player).handle();
    }

    private static World world(WorldRef world) {
        return ((FabricWorldRef) world).handle();
    }

    private static BlockPos toBlockPos(BlockPosition pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
}
