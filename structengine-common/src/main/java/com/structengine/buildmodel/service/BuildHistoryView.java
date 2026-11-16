package com.structengine.buildmodel.service;

/**
 * 给 Web/WS 展示的构建历史视图。
 */
public record BuildHistoryView(
        String historyId,
        String taskId,
        String modelName,
        long createdAt,
        int blockCount,
        boolean canRollback,
        boolean canRedo
) {
}
