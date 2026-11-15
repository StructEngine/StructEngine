package com.structengine.paper.adapter;

import com.structengine.buildmodel.spi.WorldRef;
import org.bukkit.World;

/**
 * Paper 世界句柄：包装 {@link World}。
 */
public record PaperWorldRef(World handle) implements WorldRef {
}
