package com.structengine.buildmodel.spi;

/**
 * 方块状态快照句柄：由平台实现（如 {@code BlockState} / {@code BlockData}）。
 * <p>业务层只负责透传与回写，不解析其内部结构。
 */
public interface BlockStateSnapshot {
}
