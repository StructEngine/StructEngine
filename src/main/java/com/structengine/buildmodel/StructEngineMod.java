package com.structengine.buildmodel;

import com.structengine.buildmodel.command.BuildModelCommand;
import com.structengine.buildmodel.command.RollbackCommand;
import com.structengine.buildmodel.queue.BuildTickHandler;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructEngineMod implements ModInitializer {
    public static final String MOD_ID = "structengine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        BuildModelCommand.register();
        RollbackCommand.register();
        BuildTickHandler.register();
        LOGGER.info("[StructEngine] Voxel Builder plugin initialized.");
    }
}