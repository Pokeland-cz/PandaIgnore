package me.TreeOfSelf.PandaIgnore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PandaIgnore implements ModInitializer {
    public static final String MOD_ID = "panda-ignore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("PandaIgnore loaded! Let's keep chat peaceful");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            IgnoreCommand.register(dispatcher);
            IgnoreListCommand.register(dispatcher);
        });
    }
}