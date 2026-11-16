package com.structengine.buildmodel.remote;

/**
 * 一次远程构建的进度状态。
 */
public record RemoteBuildStatus(
        String state,
        int placed,
        int total,
        String message
) {
    public static RemoteBuildStatus of(String state, int placed, int total, String message) {
        return new RemoteBuildStatus(state, placed, total, message);
    }
}
