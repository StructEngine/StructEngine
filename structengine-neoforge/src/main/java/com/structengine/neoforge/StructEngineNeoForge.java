package com.structengine.neoforge;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.buildmodel.remote.CoreCommandBridge;
import com.structengine.buildmodel.remote.GameServerWsClient;
import com.structengine.buildmodel.remote.WsSettings;
import com.structengine.neoforge.adapter.NeoForgeGameServerContext;
import com.structengine.neoforge.adapter.NeoForgeScheduler;
import com.structengine.neoforge.adapter.NeoForgeWorldAdapter;
import com.structengine.neoforge.command.BuildModelCommand;
import com.structengine.neoforge.command.RollbackCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NeoForge 平台入口：装配适配器与核心业务，注册命令与 tick 驱动。
 */
@Mod(StructEngineNeoForge.MOD_ID)
public class StructEngineNeoForge {
    public static final String MOD_ID = "structengine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static StructEngineCore core;
    private static GameServerWsClient remoteClient;
    private final NeoForgeScheduler scheduler;

    public StructEngineNeoForge(IEventBus modEventBus) {
        this.scheduler = new NeoForgeScheduler();
        core = new StructEngineCore(new NeoForgeWorldAdapter(), scheduler);
        core.registerTickHandler();
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> refreshRemotePlayers());
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> refreshRemotePlayers());
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> refreshRemotePlayers());
        LOGGER.info("[StructEngine] Voxel Builder plugin initialized (neoforge).");
    }

    private void onServerStarted(ServerStartedEvent event) {
        scheduler.bind(event.getServer());
        startRemoteClient(event.getServer());
    }

    private void startRemoteClient(MinecraftServer server) {
        try {
            WsSettings settings = WsSettings.fromEnvironment();
            if (!settings.enabled()) {
                LOGGER.info("[StructEngine] 未配置 STRUCTENGINE_WS_URL，远程构建未启用。");
                return;
            }
            remoteClient = new GameServerWsClient(settings,
                    new CoreCommandBridge(core, new NeoForgeGameServerContext(server)));
            remoteClient.start();
        } catch (Throwable e) {
            LOGGER.warn("[StructEngine] 远程 WebSocket 初始化失败", e);
        }
    }

    private static void refreshRemotePlayers() {
        if (remoteClient != null) {
            remoteClient.refreshPlayers();
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (remoteClient != null) {
            remoteClient.stop();
            remoteClient = null;
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BuildModelCommand.register(event);
        RollbackCommand.register(event);
    }

    public static StructEngineCore core() {
        if (core == null) {
            throw new IllegalStateException("StructEngine 尚未初始化");
        }
        return core;
    }
}
