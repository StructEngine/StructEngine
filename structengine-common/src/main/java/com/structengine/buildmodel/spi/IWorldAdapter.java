package com.structengine.buildmodel.spi;

import com.structengine.buildmodel.model.BlockPosition;

/**
 * 世界适配器：抽象原 Fabric 实现中直接调用 MC API 的部分——
 * 玩家位置获取、方块状态读写、消息发送。
 * <p>实现方位于各平台模块（fabric / neoforge / paper），业务层只依赖本接口。
 */
public interface IWorldAdapter {

    /** 取得玩家所在世界。 */
    WorldRef worldOf(PlayerRef player);

    /** 取得玩家所在方块坐标。 */
    BlockPosition playerBlockPos(PlayerRef player);

    /** 方块 ID 是否可解析且非空气（原 {@code Registries.BLOCK.get(id) != Blocks.AIR} 语义）。 */
    boolean isPlaceable(String blockId);

    /** 以方块默认状态放置方块（对应原 {@code setBlockState(pos, state, 3)}）。 */
    void placeBlock(WorldRef world, BlockPosition pos, String blockId);

    /** 读取方块当前状态快照（用于回滚）。 */
    BlockStateSnapshot snapshotBlock(WorldRef world, BlockPosition pos);

    /** 按快照还原方块状态（对应原 {@code setBlockState(pos, state, 3)}）。 */
    void restoreBlock(WorldRef world, BlockPosition pos, BlockStateSnapshot snapshot);

    /** 玩家是否仍在线（原 {@code !player.isDisconnected()}）。 */
    boolean isOnline(PlayerRef player);

    /** 向玩家发送彩色聊天消息。 */
    void sendMessage(PlayerRef player, String message, int color);
}
