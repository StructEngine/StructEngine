package com.structengine.buildmodel.command;

import com.structengine.buildmodel.service.BuildHistoryManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class RollbackCommand {

    private RollbackCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("rollback")
                    .executes(context -> {
                        var source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                            source.sendError(Text.literal("该指令只能由玩家执行。"));
                            return 0;
                        }
                        BuildHistoryManager.getInstance().rollback(player);
                        return 1;
                    }));

            dispatcher.register(CommandManager.literal("redo")
                    .executes(context -> {
                        var source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                            source.sendError(Text.literal("该指令只能由玩家执行。"));
                            return 0;
                        }
                        BuildHistoryManager.getInstance().redo(player);
                        return 1;
                    }));
        });
    }
}