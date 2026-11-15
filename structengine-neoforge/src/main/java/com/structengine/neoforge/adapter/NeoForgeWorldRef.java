package com.structengine.neoforge.adapter;

import com.structengine.buildmodel.spi.WorldRef;
import net.minecraft.world.level.Level;

/**
 * NeoForge 世界句柄：包装 {@link Level}。
 */
public record NeoForgeWorldRef(Level handle) implements WorldRef {
}
