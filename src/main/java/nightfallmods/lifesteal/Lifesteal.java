package nightfallmods.lifesteal;

import nightfallmods.lifesteal.command.Deathban;
import nightfallmods.lifesteal.command.Revive;
import nightfallmods.lifesteal.command.Withdraw;
import nightfallmods.lifesteal.config.CustomRecipeLoader;
import nightfallmods.lifesteal.config.EnchantmentsConfig;
import nightfallmods.lifesteal.config.ServerConfig;
import nightfallmods.lifesteal.item.Items;
import nightfallmods.lifesteal.manager.CraftedHeartTracker;
import nightfallmods.lifesteal.manager.DeathEventHandler;
import nightfallmods.lifesteal.manager.EGAEffectStripper;
import nightfallmods.lifesteal.manager.EliminatedPlayersTracker;
import nightfallmods.lifesteal.manager.GracePeriodManager;
import nightfallmods.lifesteal.manager.InventoryEnforcer;
import nightfallmods.lifesteal.manager.RevivedPlayersManager;
import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Lifesteal implements ModInitializer {

    public static final String MOD_ID = Constants.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Lifesteal] Initializing...");

        ServerConfig.getInstance();
        CustomRecipeLoader.writeDefaultTemplates();
        Items.registerModItems();
        DeathEventHandler.register();
        GracePeriodManager.register();
        RevivedPlayersManager.register();
        EGAEffectStripper.register();
        Withdraw.register();
        Revive.register();
        Deathban.register();
        InventoryEnforcer.register();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RevivedPlayersManager.setDataPath(FabricLoader.getInstance().getConfigDir());
            EliminatedPlayersTracker.setDataPath(FabricLoader.getInstance().getConfigDir());
            CraftedHeartTracker.setDataPath(FabricLoader.getInstance().getConfigDir());
            GracePeriodManager.setDataPath(FabricLoader.getInstance().getConfigDir());
            EnchantmentsConfig.init(server.registryAccess());
            UniqueItemManager.onServerStarted(server);
        });

        LOGGER.info("[Lifesteal] Ready.");
    }
}
