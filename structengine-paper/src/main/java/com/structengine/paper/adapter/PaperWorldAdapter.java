package com.structengine.paper.adapter;

import com.structengine.buildmodel.model.BlockPosition;
import com.structengine.buildmodel.spi.BlockStateSnapshot;
import com.structengine.buildmodel.spi.IWorldAdapter;
import com.structengine.buildmodel.spi.PlayerRef;
import com.structengine.buildmodel.spi.WorldRef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Paper 世界适配器：用 Bukkit API 实现与 Fabric / NeoForge 相同的能力。
 */
public class PaperWorldAdapter implements IWorldAdapter {

    @Override
    public WorldRef worldOf(PlayerRef player) {
        return new PaperWorldRef(player(player).getWorld());
    }

    @Override
    public BlockPosition playerBlockPos(PlayerRef player) {
        Location location = player(player).getLocation();
        return new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public boolean isPlaceable(String blockId) {
        return resolveMaterial(blockId) != null;
    }

    @Override
    public void placeBlock(WorldRef world, BlockPosition pos, String blockId) {
        Material material = resolveMaterial(blockId);
        if (material == null) {
            return;
        }
        block(world, pos).setType(material, false);
    }

    @Override
    public BlockStateSnapshot snapshotBlock(WorldRef world, BlockPosition pos) {
        return new PaperBlockStateSnapshot(block(world, pos).getBlockData());
    }

    @Override
    public void restoreBlock(WorldRef world, BlockPosition pos, BlockStateSnapshot snapshot) {
        block(world, pos).setBlockData(((PaperBlockStateSnapshot) snapshot).handle(), false);
    }

    @Override
    public boolean isOnline(PlayerRef player) {
        return player(player).isOnline();
    }

    @Override
    public void sendMessage(PlayerRef player, String message, int color) {
        player(player).sendMessage(Component.text(message).color(TextColor.color(color & 0xFFFFFF)));
    }

    private static Material resolveMaterial(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        // 与原实现一致：id 非法 / 非方块 / 空气一律跳过
        Material material = Material.matchMaterial(blockId);
        if (material == null || !material.isBlock() || material == Material.AIR) {
            return null;
        }
        return material;
    }

    private static Player player(PlayerRef player) {
        return ((PaperPlayerRef) player).handle();
    }

    private static Block block(WorldRef world, BlockPosition pos) {
        return ((PaperWorldRef) world).handle().getBlockAt(pos.x(), pos.y(), pos.z());
    }
}
