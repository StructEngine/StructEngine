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
import net.minecraft.world.level.levelgen.Heightmap;

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
    public int highestBlockYAt(WorldRef world, int x, int z) {
        Level level = world(world);
        // 主线程同步加载区块，避免未加载区块返回世界底部。
        level.getChunk(x >> 4, z >> 4);
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        return Math.max(level.getMinY(), top);
    }

    @Override
    public boolean isOnline(PlayerRef player) {
        return player instanceof NeoForgePlayerRef ref && !ref.handle().hasDisconnected();
    }

    @Override
    public void sendMessage(PlayerRef player, String message, int color) {
        if (!(player instanceof NeoForgePlayerRef ref)) {
            // 远程 BUILD 的玩家可能离线；构建继续执行，仅跳过聊天反馈。
            return;
        }
        ref.handle().sendSystemMessage(Component.literal(message).withColor(color));
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
        if (player instanceof NeoForgePlayerRef ref) {
            return ref.handle();
        }
        throw new IllegalArgumentException("离线玩家没有在线世界位置");
    }

    private static Level world(WorldRef world) {
        return ((NeoForgeWorldRef) world).handle();
    }

    private static BlockPos toBlockPos(BlockPosition pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
}
