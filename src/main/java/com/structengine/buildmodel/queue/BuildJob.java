package com.structengine.buildmodel.queue;

public interface BuildJob {
    boolean isDone();

    boolean processNext();

    void notifyComplete();

    void notifyAborted(String reason);
}