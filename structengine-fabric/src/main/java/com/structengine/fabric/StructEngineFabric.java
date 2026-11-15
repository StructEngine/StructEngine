package com.structengine.fabric;

import com.structengine.buildmodel.StructEngineCore;
import com.structengine.fabric.adapter.FabricScheduler;
import com.structengine.fabric.adapter.FabricWorldAdapter;
import com.structengine.fabric.command.BuildModelCommand;
import com.structengine.fabric.command.RollbackCommand;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric 平台入口：装配适配器与核心业务，注册命令与 tick 驱动。
 */
public class StructEngineFabric implements ModInitializer {
    public static final String MOD_ID = "structengine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static StructEngineCore core;

    @Override
    public void onInitialize() {
        core = new StructEngineCore(new FabricWorldAdapter(), new FabricScheduler());
        core.registerTickHandler();
        BuildModelCommand.register();
        RollbackCommand.register();
        LOGGER.info("[StructEngine] Voxel Builder plugin initialized (fabric).");
    }

    public static StructEngineCore core() {
        if (core == null) {
            throw new IllegalStateException("StructEngine 尚未初始化");
        }
        return core;
    }
}
