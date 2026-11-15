package com.structengine.fabric.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.structengine.fabric.StructEngineFabric;
import com.structengine.fabric.adapter.FabricPlayerRef;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Fabric 命令层：只做参数解析与玩家判定，业务全部委托给 common 的 {@code ModelBuildService}。
 */
public class BuildModelCommand {

    private BuildModelCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("buildmodel")
                        .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(context -> {
                                    var source = context.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
                                        source.sendError(Text.literal("该指令只能由玩家执行。"));
                                        return 0;
                                    }
                                    String url = StringArgumentType.getString(context, "url");
                                    StructEngineFabric.core().buildService()
                                            .startBuild(new FabricPlayerRef(player), url);
                                    return 1;
                                }))));
    }
}
