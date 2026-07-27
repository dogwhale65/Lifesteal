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
import net.minecraft.world.entity.player.Player;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Post-death protection from other players. Expiries are wall-clock timestamps so the countdown
 * keeps running while a player is offline and survives a restart.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace.json";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Stops an attacker who is spamming a protected player from flooding their own chat. */
    private static final long NOTICE_COOLDOWN_TICKS = 20L;

    private static final Map<UUID, Long> expiries   = new HashMap<>();
    private static final Map<UUID, Long> lastNotice = new HashMap<>();
    private static Path dataPath;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::onDamage);
        Lifesteal.LOGGER.info("[Grace] Grace period handler registered.");
    }

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    /** Starts a fresh grace period for a player who has just respawned. */
    public static void start(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        // Respawning already armoured (keepInventory) would end the period on the very next tick,
        // so skip it entirely rather than announcing a period and revoking it in the same second.
        if (isWearingArmor(player)) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        save();

        player.sendSystemMessage(Component.literal(
                "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                        + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    public static boolean isProtected(Player player) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return false;
        Long until = expiries.get(player.getUUID());
        return until != null && System.currentTimeMillis() < until;
    }

    /** Ends the period and tells the player, whatever the reason. */
    public static void end(ServerPlayer player) {
        if (expiries.remove(player.getUUID()) == null) return;
        lastNotice.remove(player.getUUID());
        save();
        player.sendSystemMessage(
                Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    private static void onTick(MinecraftServer server) {
        if (expiries.isEmpty()) return;

        // Turning the feature off drops everyone's protection silently rather than letting stale
        // entries fire an "ended" message later.
        if (!ServerConfig.getInstance().gracePeriodEnabled) {
            expiries.clear();
            lastNotice.clear();
            save();
            return;
        }

        long now = System.currentTimeMillis();

        for (UUID id : List.copyOf(expiries.keySet())) {
            Long until = expiries.get(id);
            if (until == null) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (now >= until) {
                if (player != null) {
                    end(player);
                } else {
                    expiries.remove(id);
                    save();
                }
            } else if (player != null && isWearingArmor(player)) {
                end(player);
            }
        }
    }

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return true;
        if (!(source.getEntity() instanceof Player attacker) || attacker == entity) return true;

        // Throwing the first punch costs you your own protection, even if the hit is blocked below.
        if (entity instanceof Player && attacker instanceof ServerPlayer serverAttacker
                && isProtected(serverAttacker)) {
            end(serverAttacker);
        }

        if (!(entity instanceof ServerPlayer victim) || !isProtected(victim)) return true;

        if (attacker instanceof ServerPlayer serverAttacker) notifyProtected(serverAttacker, victim);
        return false;
    }

    private static void notifyProtected(ServerPlayer attacker, ServerPlayer victim) {
        long now = attacker.level().getGameTime();
        Long last = lastNotice.get(attacker.getUUID());
        if (last != null && now - last < NOTICE_COOLDOWN_TICKS) return;

        lastNotice.put(attacker.getUUID(), now);
        attacker.sendSystemMessage(Component.literal(
                victim.getName().getString() + " is still in their grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /** Renders a duration in the largest whole unit it divides into: 1800 -> "30 minutes". */
    public static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60   && seconds % 60   == 0) return plural(seconds / 60,   "minute");
        return plural(seconds, "second");
    }

    private static String plural(int value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            Map<UUID, Long> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                expiries.clear();
                expiries.putAll(loaded);
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
                GSON.toJson(expiries, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to save grace periods. ({})", e.getMessage());
        }
    }
}
