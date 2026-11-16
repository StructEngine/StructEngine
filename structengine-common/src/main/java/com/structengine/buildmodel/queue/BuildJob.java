package com.structengine.buildmodel.queue;

import java.util.UUID;

public interface BuildJob {

    /** 所属玩家；null 表示未归属（兼容旧任务）。 */
    default UUID playerId() {
        return null;
    }

    boolean isDone();

    boolean processNext();

    void notifyComplete();

    void notifyAborted(String reason);
}