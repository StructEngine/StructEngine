package com.structengine.neoforge.adapter;

import com.structengine.buildmodel.spi.BlockStateSnapshot;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NeoForge 方块状态快照：包装 {@link BlockState}。
 */
public record NeoForgeBlockStateSnapshot(BlockState handle) implements BlockStateSnapshot {
}
