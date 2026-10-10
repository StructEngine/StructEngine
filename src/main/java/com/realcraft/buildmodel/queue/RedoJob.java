package com.realcraft.buildmodel.queue;

import com.realcraft.buildmodel.model.BlockPlacement;
import com.realcraft.buildmodel.service.BuildHistoryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

public class RedoJob implements BuildJob {
    private final ServerPlayerEntity player;
    private final World world;
    private final List<BlockPlacement> placements;
    private int index;

    public RedoJob(ServerPlayerEntity player, World world, List<BlockPlacement> placements) {
        this.player = player;
        this.world = world;
        this.placements = placements;
        this.index = 0;
    }

    @Override
    public boolean isDone() {
        return index >= placements.size();
    }

    @Override
    public boolean processNext() {
        if (isDone()) {
            return false;
        }
        BlockPlacement placement = placements.get(index++);
        world.setBlockState(placement.getPos(), placement.getState(), 3);
        return true;
    }

    @Override
    public void notifyComplete() {
        BuildHistoryManager.getInstance().onRestoreComplete(player.getUuid());
        if (!player.isDisconnected()) {
            player.sendMessage(Text.literal("恢复完成！已重新放置 " + placements.size() + " 个方块。").withColor(0xFF55FF55));
        }
    }

    @Override
    public void notifyAborted(String reason) {
        BuildHistoryManager.getInstance().onRestoreComplete(player.getUuid());
        if (!player.isDisconnected()) {
            player.sendMessage(Text.literal("[redo] 恢复中断: " + reason).withColor(0xFFFF5555));
        }
    }
}