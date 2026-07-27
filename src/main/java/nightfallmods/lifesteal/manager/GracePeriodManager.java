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
import java.util.Map;
import java.util.UUID;

/**
 * Post-death protection from other players. Expiry is stored as an absolute server game-time tick so
 * the countdown survives a restart and does not burn away while the server is stopped.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace-periods.json";

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final long TICKS_PER_SECOND = 20L;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final Map<UUID, Long> expiryTick = new HashMap<>();
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

    /** Starts the grace period after a death. No-op when the feature is switched off. */
    public static void grant(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        expiryTick.put(player.getUUID(),
                player.level().getGameTime() + cfg.gracePeriodSeconds * TICKS_PER_SECOND);
        save();

        player.sendSystemMessage(Component.literal(
                        "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                                + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    public static boolean isProtected(ServerPlayer player) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return false;
        Long until = expiryTick.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    /** Ends the grace period for any reason — expiry, armor, or throwing the first punch. */
    public static void end(ServerPlayer player) {
        if (expiryTick.remove(player.getUUID()) == null) return;
        save();
        player.sendSystemMessage(
                Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    private static void onTick(MinecraftServer server) {
        if (expiryTick.isEmpty()) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!expiryTick.containsKey(player.getUUID())) continue;
            if (!isProtected(player) || isWearingArmor(player)) end(player);
        }
    }

    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;

        // Swinging at another player forfeits your own protection, hit or no hit.
        if (entity instanceof Player && entity != attacker) end(attacker);

        if (!(entity instanceof ServerPlayer victim) || victim == attacker) return true;
        if (!isProtected(victim)) return true;

        attacker.sendSystemMessage(
                Component.literal(victim.getName().getString() + " is still in their grace period.")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        return false;
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /** Renders a duration the way the grant message reads it: "30 minutes", "1 hour", "90 seconds". */
    public static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60 && seconds % 60 == 0)     return plural(seconds / 60, "minute");
        if (seconds < 60)                           return plural(seconds, "second");
        return plural(seconds / 60, "minute") + " " + plural(seconds % 60, "second");
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
                expiryTick.clear();
                expiryTick.putAll(loaded);
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
                GSON.toJson(expiryTick, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to save grace periods. ({})", e.getMessage());
        }
    }
}
