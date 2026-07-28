package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A post-respawn window during which a player takes no damage from other players.
 *
 * <p>It is meant to cover rebuilding after a death, so it ends the moment the player stops being a
 * soft target: when they armour up, when they throw a punch of their own, or when the configured
 * duration runs out. Expiries are stored as wall-clock timestamps and persisted, so logging out
 * cannot be used to bank the remaining protection.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace.json";
    private static final int CHECK_INTERVAL_TICKS = 20;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<UUID, Long> expiries = new HashMap<>();
    private static Path dataPath;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::onDamage);
        Lifesteal.LOGGER.info("[Grace] Manager registered.");
    }

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    /** Starts the window for a freshly respawned player, if the feature is switched on. */
    public static void begin(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        save();

        player.sendSystemMessage(Component.literal(
                "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                        + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    private static boolean isProtected(ServerPlayer player) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return false;

        Long expiry = expiries.get(player.getUUID());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            end(player);
            return false;
        }
        return true;
    }

    /** Ends the window and tells the player, without saying which of the three reasons it was. */
    private static void end(ServerPlayer player) {
        if (expiries.remove(player.getUUID()) == null) return;
        save();
        player.sendSystemMessage(Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    /**
     * Blocks player-initiated damage against a protected victim, and drops the attacker's own
     * protection for having swung — the punch counts whether or not it was allowed to land.
     */
    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return true;
        if (!(entity instanceof ServerPlayer victim)) return true;
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == victim) return true;

        end(attacker);

        if (!isProtected(victim)) return true;
        attacker.sendSystemMessage(Component.literal(
                victim.getName().getString() + " is still in their grace period.")
                .withStyle(ChatFormatting.YELLOW));
        return false;
    }

    private static void onTick(MinecraftServer server) {
        if (expiries.isEmpty()) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Long expiry = expiries.get(player.getUUID());
            if (expiry == null) continue;
            if (now >= expiry || isWearingArmor(player)) end(player);
        }
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /** Renders the configured duration in the largest unit it divides into cleanly. */
    private static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60   && seconds % 60   == 0) return plural(seconds / 60,   "minute");
        return plural(seconds, "second");
    }

    private static String plural(int amount, String unit) {
        return amount + " " + unit + (amount == 1 ? "" : "s");
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            Map<UUID, Long> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                expiries.clear();
                expiries.putAll(loaded);
                expiries.values().removeIf(expiry -> System.currentTimeMillis() >= expiry);
                Lifesteal.LOGGER.info("[Grace] Loaded {} active grace period(s).", expiries.size());
            }
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to load. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(expiries, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to save. ({})", e.getMessage());
        }
    }
}
