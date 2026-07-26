package nightfallmods.lifesteal;

import nightfallmods.lifesteal.config.ClientSettingsManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifestealClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("lifesteal");

    @Override
    public void onInitializeClient() {
        ClientSettingsManager.getInstance();

        LOGGER.info("[Lifesteal] Client initialized.");
    }
}

