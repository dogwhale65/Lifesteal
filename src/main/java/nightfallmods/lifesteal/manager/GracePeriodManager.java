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
 * Post-death protection from other players. Grace ends when it runs out, when the player puts
 * on armour, or when they attack another player — so it shields a fresh respawn without
 * letting anyone fight from behind it.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace.json";

    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final long NOTIFY_COOLDOWN_MS = 1000L;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<UUID, Long> expiresAt   = new HashMap<>();
    private static final Map<UUID, Long> lastNotified = new HashMap<>();

    private static Path dataPath;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::onDamage);
        Lifesteal.LOGGER.info("[Grace] Grace period handler registered.");
    }

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    /** Starts a grace period for a freshly respawned player, if the feature is enabled. */
    public static void start(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        expiresAt.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        save();

        player.sendSystemMessage(Component.literal(
                "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                        + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    public static boolean isProtected(ServerPlayer player) {
        Long expiry = expiresAt.get(player.getUUID());
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    /** Ends the grace period and tells the player, if they had one. */
    public static void end(ServerPlayer player) {
        if (expiresAt.remove(player.getUUID()) == null) return;
        save();
        player.sendSystemMessage(
                Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    /** Renders a duration the way it reads in chat, e.g. {@code 1800 -> "30 minutes"}. */
    public static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60 && seconds % 60 == 0)     return plural(seconds / 60, "minute");
        if (seconds < 60)                           return plural(seconds, "second");
        return plural(seconds / 60, "minute") + " " + plural(seconds % 60, "second");
    }

    private static String plural(int value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;
        if (!(entity instanceof ServerPlayer victim) || victim == attacker) return true;

        // Swinging at another player forfeits your own protection, hit or not.
        end(attacker);

        if (!isProtected(victim)) return true;
        notifyAttacker(attacker, victim);
        return false;
    }

    private static void onTick(MinecraftServer server) {
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        if (expiresAt.isEmpty()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!expiresAt.containsKey(player.getUUID())) continue;
            if (!isProtected(player) || isWearingArmor(player)) end(player);
        }

        long now = System.currentTimeMillis();
        if (expiresAt.values().removeIf(expiry -> now >= expiry)) save();
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    private static void notifyAttacker(ServerPlayer attacker, ServerPlayer victim) {
        long now = System.currentTimeMillis();
        Long last = lastNotified.get(attacker.getUUID());
        if (last != null && now - last < NOTIFY_COOLDOWN_MS) return;
        lastNotified.put(attacker.getUUID(), now);

        attacker.sendSystemMessage(Component.literal(
                victim.getName().getString() + " is still in their grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            Map<UUID, Long> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                expiresAt.clear();
                expiresAt.putAll(loaded);
            }
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to load grace periods. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(expiresAt, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to save grace periods. ({})", e.getMessage());
        }
    }
}
