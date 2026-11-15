package com.structengine.buildmodel;

import com.structengine.buildmodel.queue.BuildTaskQueue;
import com.structengine.buildmodel.queue.BuildTickHandler;
import com.structengine.buildmodel.service.BuildHistoryManager;
import com.structengine.buildmodel.service.ModelBuildService;
import com.structengine.buildmodel.service.ModelDownloader;
import com.structengine.buildmodel.spi.IServerScheduler;
import com.structengine.buildmodel.spi.IWorldAdapter;

/**
 * 组合根（Composition Root）：把平台适配器与平台无关的核心业务装配在一起。
 * <p>各平台模块在启动时创建唯一实例，并把它的 service 交给命令层使用。
 */
public final class StructEngineCore {

    private final IWorldAdapter worldAdapter;
    private final IServerScheduler scheduler;
    private final BuildTaskQueue taskQueue;
    private final BuildHistoryManager historyManager;
    private final ModelDownloader downloader;
    private final ModelBuildService buildService;

    public StructEngineCore(IWorldAdapter worldAdapter, IServerScheduler scheduler) {
        this.worldAdapter = worldAdapter;
        this.scheduler = scheduler;
        this.taskQueue = new BuildTaskQueue();
        this.historyManager = new BuildHistoryManager(worldAdapter, taskQueue);
        this.downloader = new ModelDownloader();
        this.buildService = new ModelBuildService(downloader, worldAdapter, scheduler,
                historyManager, taskQueue);
    }

    /** 注册每 tick 的分批放置驱动（原 {@code BuildTickHandler.register()}）。 */
    public void registerTickHandler() {
        BuildTickHandler.register(scheduler, taskQueue);
    }

    public IWorldAdapter worldAdapter() {
        return worldAdapter;
    }

    public IServerScheduler scheduler() {
        return scheduler;
    }

    public BuildTaskQueue taskQueue() {
        return taskQueue;
    }

    public BuildHistoryManager historyManager() {
        return historyManager;
    }

    public ModelBuildService buildService() {
        return buildService;
    }

    /** 供远程构建复用同一个 HTTP 下载器（连接池与超时策略保持一致）。 */
    public ModelDownloader downloader() {
        return downloader;
    }
}
