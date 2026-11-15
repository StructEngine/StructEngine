package com.structengine.buildmodel.model;

/**
 * 平台无关的方块坐标（替代原 {@code net.minecraft.util.math.BlockPos}）。
 */
public record BlockPosition(int x, int y, int z) {
}
