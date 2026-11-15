package com.structengine.neoforge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.structengine.neoforge.StructEngineNeoForge;
import com.structengine.neoforge.adapter.NeoForgePlayerRef;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * NeoForge 命令层：只做参数解析与玩家判定，业务委托给 common。
 */
public class BuildModelCommand {

    private BuildModelCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("buildmodel")
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                context.getSource().sendFailure(Component.literal("该指令只能由玩家执行。"));
                                return 0;
                            }
                            String url = StringArgumentType.getString(context, "url");
                            StructEngineNeoForge.core().buildService()
                                    .startBuild(new NeoForgePlayerRef(player), url);
                            return 1;
                        })));
    }
}
