package com.structengine.buildmodel.model;

/**
 * 一次待放置的方块：坐标 + 方块 ID。
 * <p>原实现直接持有 MC 的 {@code BlockPos} / {@code BlockState}，
 * 现改为持平台无关坐标与方块 ID，由 {@code IWorldAdapter} 在放置时解析状态。
 */
public class BlockPlacement {
    private final BlockPosition pos;
    private final String blockId;

    public BlockPlacement(BlockPosition pos, String blockId) {
        this.pos = pos;
        this.blockId = blockId;
    }

    public BlockPosition getPos() {
        return pos;
    }

    public String getBlockId() {
        return blockId;
    }
}
