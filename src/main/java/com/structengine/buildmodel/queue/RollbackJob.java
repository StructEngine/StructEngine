package com.structengine.buildmodel.queue;

import com.structengine.buildmodel.model.RegionSnapshot;
import com.structengine.buildmodel.service.BuildHistoryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class RollbackJob implements BuildJob {
    private final ServerPlayerEntity player;
    private final World world;
    private final RegionSnapshot snapshot;

    public RollbackJob(ServerPlayerEntity player, World world, RegionSnapshot snapshot) {
        this.player = player;
        this.world = world;
        this.snapshot = snapshot;
        this.snapshot.reset();
    }

    @Override
    public boolean isDone() {
        return snapshot.isFullyRestored();
    }

    @Override
    public boolean processNext() {
        return snapshot.restoreNext(world);
    }

    @Override
    public void notifyComplete() {
        BuildHistoryManager.getInstance().onRestoreComplete(player.getUuid());
        if (!player.isDisconnected()) {
            player.sendMessage(Text.literal("回滚完成！已恢复 " + snapshot.getTotal() + " 个方块。").withColor(0xFF55FF55));
        }
    }

    @Override
    public void notifyAborted(String reason) {
        BuildHistoryManager.getInstance().onRestoreComplete(player.getUuid());
        if (!player.isDisconnected()) {
            player.sendMessage(Text.literal("[rollback] 回滚中断: " + reason).withColor(0xFFFF5555));
        }
    }
}