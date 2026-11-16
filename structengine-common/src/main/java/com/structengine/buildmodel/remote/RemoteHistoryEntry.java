package com.structengine.buildmodel.remote;

/**
 * 插件构建历史快照，供 Web 回滚/恢复列表展示。
 */
public record RemoteHistoryEntry(
        String historyId,
        String taskId,
        String modelName,
        long createdAt,
        int blockCount,
        boolean canRollback,
        boolean canRedo
) {
}
