package com.structengine.paper.adapter;

import com.structengine.buildmodel.spi.BlockStateSnapshot;
import org.bukkit.block.data.BlockData;

/**
 * Paper 方块状态快照：包装 {@link BlockData}。
 */
public record PaperBlockStateSnapshot(BlockData handle) implements BlockStateSnapshot {
}
