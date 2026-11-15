package com.structengine.paper;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.paper.adapter.PaperScheduler;
import com.structengine.paper.adapter.PaperWorldAdapter;
import com.structengine.paper.command.BuildModelCommand;
import com.structengine.paper.command.RollbackCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper 平台入口：装配适配器与核心业务，注册命令与 tick 驱动。
 */
public class StructEnginePaperPlugin extends JavaPlugin {

    private StructEngineCore core;

    @Override
    public void onEnable() {
        core = new StructEngineCore(new PaperWorldAdapter(), new PaperScheduler(this));
        core.registerTickHandler();
        register("buildmodel", new BuildModelCommand(core.buildService()));
        register("rollback", new RollbackCommand(core.historyManager()));
        register("redo", new RollbackCommand(core.historyManager()));
        getLogger().info("[StructEngine] Voxel Builder plugin initialized (paper).");
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
