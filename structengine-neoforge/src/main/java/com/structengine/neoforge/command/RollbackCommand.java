package com.structengine.neoforge.command;

import com.structengine.neoforge.StructEngineNeoForge;
import com.structengine.neoforge.adapter.NeoForgePlayerRef;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * NeoForge 命令层：/rollback 与 /redo。
 */
public class RollbackCommand {

    private RollbackCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollback")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("该指令只能由玩家执行。"));
                        return 0;
                    }
                    StructEngineNeoForge.core().historyManager().rollback(new NeoForgePlayerRef(player));
                    return 1;
                }));

        event.getDispatcher().register(Commands.literal("redo")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendFailure(Component.literal("该指令只能由玩家执行。"));
                        return 0;
                    }
                    StructEngineNeoForge.core().historyManager().redo(new NeoForgePlayerRef(player));
                    return 1;
                }));
    }
}
