package com.structengine.paper;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.buildmodel.remote.CoreCommandBridge;
import com.structengine.buildmodel.remote.GameServerWsClient;
import com.structengine.buildmodel.remote.WsSettings;
import com.structengine.paper.adapter.PaperGameServerContext;
import com.structengine.paper.adapter.PaperScheduler;
import com.structengine.paper.adapter.PaperWorldAdapter;
import com.structengine.paper.command.BuildModelCommand;
import com.structengine.paper.command.RollbackCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Paper 平台入口：装配适配器与核心业务，注册命令与 tick 驱动。
 */
public class StructEnginePaperPlugin extends JavaPlugin {

    private StructEngineCore core;
    private GameServerWsClient remoteClient;

    @Override
    public void onEnable() {
        core = new StructEngineCore(new PaperWorldAdapter(), new PaperScheduler(this));
        core.registerTickHandler();
        register("buildmodel", new BuildModelCommand(core.buildService()));
        register("rollback", new RollbackCommand(core.historyManager()));
        register("redo", new RollbackCommand(core.historyManager()));
        startRemoteClient();
        registerPlayerEvents();
        getLogger().info("[StructEngine] Voxel Builder plugin initialized (paper).");
    }

    private void startRemoteClient() {
        try {
            WsSettings settings = WsSettings.fromEnvironment();
            if (!settings.enabled()) {
                getLogger().info("[StructEngine] 未配置 STRUCTENGINE_WS_URL，远程构建未启用。");
                return;
            }
            remoteClient = new GameServerWsClient(settings,
                    new CoreCommandBridge(core, new PaperGameServerContext()));
            remoteClient.start();
        } catch (Throwable e) {
            getLogger().log(Level.WARNING, "[StructEngine] 远程 WebSocket 初始化失败", e);
        }
    }

    private void registerPlayerEvents() {
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                refreshRemotePlayers();
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                refreshRemotePlayers();
            }

            @EventHandler
            public void onWorldChange(PlayerChangedWorldEvent event) {
                refreshRemotePlayers();
            }
        }, this);
    }

    private void refreshRemotePlayers() {
        if (remoteClient != null) {
            remoteClient.refreshPlayers();
        }
    }

    @Override
    public void onDisable() {
        if (remoteClient != null) {
            remoteClient.stop();
            remoteClient = null;
        }
        getLogger().info("[StructEngine] Voxel Builder plugin disabled (paper).");
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("命令未在 plugin.yml 中声明: " + name);
            return;
        }
        command.setExecutor(executor);
    }

    public StructEngineCore core() {
        return core;
    }
}
