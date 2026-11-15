package com.structengine.fabric.adapter;

import com.structengine.buildmodel.spi.WorldRef;
import net.minecraft.world.World;

/**
 * Fabric 世界句柄：包装 {@link World}。
 */
public record FabricWorldRef(World handle) implements WorldRef {
}
