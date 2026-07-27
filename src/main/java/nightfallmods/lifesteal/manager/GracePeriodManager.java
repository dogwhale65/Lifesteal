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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immunity to player-dealt damage, granted on respawn and lasting until it expires, the player
 * wears armour, or the player attacks another player.
 *
 * Expiries are absolute wall-clock times so the clock keeps running while the player is offline;
 * they are written to disk so a restart mid-grace does not silently extend it.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace.json";

    private static final EquipmentSlot[] ARMOUR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** Player UUID to the epoch millisecond at which their grace period lapses. */
    private static final Map<UUID, Long> graceUntil = new HashMap<>();
    private static Path dataPath;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::onDamage);
        Lifesteal.LOGGER.info("[Grace] Manager registered.");
    }

    public static void setDataPath(Path configDir) {
        dataPath = configDir.resolve(DATA_FILE);
        load();
    }

    /** Starts a grace period for a player who has just respawned. No-op when disabled. */
    public static void begin(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.isGracePeriodEnabled()) return;
        // Armour disqualifies, so a player who respawns still wearing it (keepInventory) is never
        // granted a grace period rather than being granted one and losing it a tick later.
        if (isWearingArmour(player)) return;

        graceUntil.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        save();

        player.sendSystemMessage(Component.literal(
                "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                        + ". Wearing armor or attacking another player will end the grace period."
        ).withStyle(ChatFormatting.YELLOW));
    }

    public static boolean isProtected(Player player) {
        Long until = graceUntil.get(player.getUUID());
        return until != null && System.currentTimeMillis() < until;
    }

    /** Ends the grace period and tells the player, once. The reason is deliberately not given. */
    public static void end(ServerPlayer player) {
        if (graceUntil.remove(player.getUUID()) == null) return;
        save();
        player.sendSystemMessage(
                Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    private static void onTick(MinecraftServer server) {
        if (graceUntil.isEmpty()) return;

        // Iterating the tracked players rather than the player list keeps this free when nobody
        // is in a grace period, which is the normal case.
        List<UUID> tracked = new ArrayList<>(graceUntil.keySet());
        for (UUID id : tracked) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                // Offline players still time out; they are simply told nothing.
                Long until = graceUntil.get(id);
                if (until != null && System.currentTimeMillis() >= until) {
                    graceUntil.remove(id);
                    save();
                }
                continue;
            }
            if (!isProtected(player) || isWearingArmour(player)) end(player);
        }
    }

    private static boolean isWearingArmour(ServerPlayer player) {
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return true;
        }
        return false;
    }

    /**
     * Blocks player-dealt damage against a protected victim, and drops the attacker's own
     * protection for having thrown the punch — landed or not.
     *
     * {@code source.getEntity()} is the entity responsible rather than the projectile, so arrows
     * and thrown potions count as player-initiated too.
     */
    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return true;

        if (entity instanceof Player && entity != attacker && isProtected(attacker)) {
            end(attacker);
        }

        if (!(entity instanceof ServerPlayer victim) || victim == attacker) return true;
        if (!isProtected(victim)) return true;

        attacker.sendSystemMessage(
                Component.literal(victim.getName().getString() + " is still in their grace period.")
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        return false;
    }

    /** Renders a duration in the largest whole unit it divides into: "30 minutes", "45 seconds". */
    public static String describe(int seconds) {
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
                graceUntil.clear();
                graceUntil.putAll(loaded);
                Lifesteal.LOGGER.info("[Grace] Loaded {} active grace period(s).", graceUntil.size());
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
                GSON.toJson(graceUntil, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Grace] Failed to save. ({})", e.getMessage());
        }
    }
}
