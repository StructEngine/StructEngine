package com.structengine.fabric;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.buildmodel.remote.CoreCommandBridge;
import com.structengine.buildmodel.remote.GameServerWsClient;
import com.structengine.buildmodel.remote.WsSettings;
import com.structengine.fabric.adapter.FabricGameServerContext;
import com.structengine.fabric.adapter.FabricScheduler;
import com.structengine.fabric.adapter.FabricWorldAdapter;
import com.structengine.fabric.command.BuildModelCommand;
import com.structengine.fabric.command.RollbackCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric 平台入口：装配适配器与核心业务，注册命令与 tick 驱动。
 */
public class StructEngineFabric implements ModInitializer {
    public static final String MOD_ID = "structengine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static StructEngineCore core;
    private static GameServerWsClient remoteClient;

    @Override
    public void onInitialize() {
        core = new StructEngineCore(new FabricWorldAdapter(), new FabricScheduler());
        core.registerTickHandler();
        BuildModelCommand.register();
        RollbackCommand.register();
        ServerLifecycleEvents.SERVER_STARTED.register(this::startRemoteClient);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> stopRemoteClient());
        LOGGER.info("[StructEngine] Voxel Builder plugin initialized (fabric).");
    }

    private void startRemoteClient(MinecraftServer server) {
        try {
            WsSettings settings = WsSettings.fromEnvironment();
            if (!settings.enabled()) {
                LOGGER.info("[StructEngine] 未配置 STRUCTENGINE_WS_URL，远程构建未启用。");
                return;
            }
            remoteClient = new GameServerWsClient(settings,
                    new CoreCommandBridge(core, new FabricGameServerContext(server)));
            remoteClient.start();
        } catch (Throwable e) {
            LOGGER.warn("[StructEngine] 远程 WebSocket 初始化失败", e);
        }
    }

    private static void stopRemoteClient() {
        if (remoteClient != null) {
            remoteClient.stop();
            remoteClient = null;
        }
    }

    public static StructEngineCore core() {
        if (core == null) {
            throw new IllegalStateException("StructEngine 尚未初始化");
        }
        return core;
    }
}
