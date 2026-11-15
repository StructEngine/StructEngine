package com.structengine.paper.command;

import com.structengine.buildmodel.service.BuildHistoryManager;
import com.structengine.paper.adapter.PaperPlayerRef;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Paper 命令层：同时处理 /rollback 与 /redo。
 */
public class RollbackCommand implements CommandExecutor {

    private final BuildHistoryManager historyManager;

    public RollbackCommand(BuildHistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该指令只能由玩家执行。");
            return true;
        }
        if ("redo".equalsIgnoreCase(command.getName())) {
            historyManager.redo(new PaperPlayerRef(player));
        } else {
            historyManager.rollback(new PaperPlayerRef(player));
        }
        return true;
    }
}
