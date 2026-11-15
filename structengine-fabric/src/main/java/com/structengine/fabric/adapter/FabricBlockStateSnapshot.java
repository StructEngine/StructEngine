package com.structengine.fabric.adapter;

import com.structengine.buildmodel.spi.BlockStateSnapshot;
import net.minecraft.block.BlockState;

/**
 * Fabric 方块状态快照：包装 {@link BlockState}。
 */
public record FabricBlockStateSnapshot(BlockState handle) implements BlockStateSnapshot {
}
