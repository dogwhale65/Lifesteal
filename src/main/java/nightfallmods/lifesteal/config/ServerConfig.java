package nightfallmods.lifesteal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import nightfallmods.lifesteal.Constants;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "lifesteal.json";

    private static ServerConfig instance;

    public enum DeathBanType { BAN, SPECTATOR }
    public enum WithdrawFullInventoryAction { PREVENT, DROP }
    public enum CraftedHeartWithdrawAction { SPECIFIC, CAP, NONE }

    public int maxHearts         = 20;
    public int startingHearts    = 10;
    public int craftedHeartCap   = 10;
    public int reviveHearts      = 3;

    public boolean minimumHeartsEnabled = false;
    public int     minimumHearts        = 1;

    public boolean egaHeartLimitEnabled = false;
    public int egaHeartThreshold = 12;

    public String deathBanType = DeathBanType.BAN.name();

    public String finalDeathSound            = Constants.SOUND_ELIMINATION;
    public String heartEquipSound            = Constants.SOUND_HEART_EQUIP;
    public String heartDeathSound            = Constants.SOUND_HEART_DEATH;
    public int    heartEquipSoundChunkRadius = 4;

    public boolean gracePeriodEnabled = false;
    public int     gracePeriodSeconds = 1800;

    public boolean fullHeartOnGain = false;

    public String withdrawFullInventoryAction = WithdrawFullInventoryAction.DROP.name();

    public String craftedHeartWithdrawAction = CraftedHeartWithdrawAction.SPECIFIC.name();
    public int craftedHeartWithdrawCap = 10;

    public java.util.Map<String, String> customRecipes = new java.util.TreeMap<>(java.util.Map.of(
            "lifesteal:heart_fragment", "heart_fragment.json",
            "lifesteal:crafted_heart", "crafted_heart.json",
            "lifesteal:beacon_of_life", "beacon_of_life.json"));

    public java.util.List<String> bannedItems = new java.util.ArrayList<>();

    public java.util.List<String> uniqueItems = new java.util.ArrayList<>(
            java.util.List.of("minecraft:mace", "minecraft:netherite_chestplate"));
    public boolean uniqueDragonEgg = true;

    public boolean disableTotems      = false;
    public boolean disableEnderPearls = false;
    public boolean disableElytras     = false;
    public boolean disableWindCharges = false;
    public boolean disableEGA         = false;
    public boolean disableTNTCarts    = false;

    public java.util.List<String> blockedEffects = new java.util.ArrayList<>(
            java.util.List.of("minecraft:luck 3"));

    public static ServerConfig getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    public static void reload() {
        instance = load();
        Lifesteal.LOGGER.info("[Config] Reloaded.");
    }

    private static ServerConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                ServerConfig cfg = GSON.fromJson(reader, ServerConfig.class);
                if (cfg != null) {

                    cfg.save();
                    Lifesteal.LOGGER.info("[Config] Loaded from disk.");
                    return cfg;
                }
            } catch (IOException | JsonParseException e) {
                Lifesteal.LOGGER.error("[Config] Failed to load — using defaults. ({})", e.getMessage());
            }
        }
        ServerConfig defaults = new ServerConfig();
        defaults.save();
        Lifesteal.LOGGER.info("[Config] No config found — wrote defaults.");
        return defaults;
    }

    public void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.createDirectories(path.getParent());
            try (FileWriter writer = new FileWriter(path.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Config] Failed to save. ({})", e.getMessage());
        }
    }

    public boolean isEffectBlocked(String effectId, int amplifier) {
        for (String entry : blockedEffects) {
            String[] parts = entry.trim().split("\\s+");
            if (parts.length == 0 || !parts[0].equals(effectId)) continue;
            if (parts.length == 1) return true;
            try {
                return amplifier + 1 >= Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

    public double getMaxHealth()      { return maxHearts      * Constants.HEART_VALUE; }
    public double getStartingHealth() { return startingHearts * Constants.HEART_VALUE; }
    public double getReviveHealth()   { return reviveHearts   * Constants.HEART_VALUE; }

    /** Lowest heart count a player can be reduced to. One heart when the floor is off — nobody sits at zero. */
    public int heartFloor() {
        return minimumHeartsEnabled ? Math.max(1, minimumHearts) : 1;
    }

    public double getMinimumHealth() { return heartFloor() * Constants.HEART_VALUE; }

    /** True when taking one more heart off {@code health} would drop the player under the configured floor. */
    public boolean isProtectedByHeartFloor(double health) {
        return minimumHeartsEnabled && health - Constants.HEART_VALUE < getMinimumHealth();
    }

    public CraftedHeartWithdrawAction craftedHeartWithdrawMode() {
        for (CraftedHeartWithdrawAction mode : CraftedHeartWithdrawAction.values()) {
            if (mode.name().equalsIgnoreCase(craftedHeartWithdrawAction)) return mode;
        }
        return CraftedHeartWithdrawAction.SPECIFIC;
    }

    public boolean isWithdrawActionPrevent() {
        return WithdrawFullInventoryAction.PREVENT.name().equalsIgnoreCase(withdrawFullInventoryAction);
    }

    public boolean isDeathBanTypeBan() {
        return DeathBanType.BAN.name().equalsIgnoreCase(deathBanType);
    }
    public boolean isDeathBanTypeSpectator() {
        return DeathBanType.SPECTATOR.name().equalsIgnoreCase(deathBanType);
    }
}
