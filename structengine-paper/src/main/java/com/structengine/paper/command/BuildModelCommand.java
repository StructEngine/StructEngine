package com.structengine.paper.command;

import com.structengine.buildmodel.service.ModelBuildService;
import com.structengine.paper.adapter.PaperPlayerRef;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Paper 命令层：/buildmodel &lt;url&gt;，URL 允许含空格。
 */
public class BuildModelCommand implements CommandExecutor {

    private final ModelBuildService buildService;

    public BuildModelCommand(ModelBuildService buildService) {
        this.buildService = buildService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该指令只能由玩家执行。");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("用法: /buildmodel <url>");
            return true;
        }
        buildService.startBuild(new PaperPlayerRef(player), String.join(" ", args));
        return true;
    }
}
