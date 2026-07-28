package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

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
 * A window of immunity to player-initiated damage, granted on respawn when the feature is enabled.
 *
 * <p>The window is a wall-clock deadline rather than a tick countdown, so it survives logouts and
 * restarts the way a player expects a "30 minutes" promise to. It is deliberately easy to give up:
 * putting on armour or swinging at another player both end it immediately, so protection cannot be
 * used as a shield while gearing up or fighting.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace-periods.json";

    private static final int SCAN_INTERVAL_TICKS = 20;

    /** How long an attacker waits between repeats of the "still in their grace period" notice. */
    private static final long NOTICE_INTERVAL_MS = 2_000L;

    private static final Map<UUID, Long> expiries = new HashMap<>();
    private static final Map<UUID, Long> lastNotice = new HashMap<>();

    private static Path dataPath;
    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::onDamage);

        // A window that ran out while the player was away is simply gone; announcing its end days
        // later would be noise, so it is dropped without the message the live path would send.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID id = handler.getPlayer().getUUID();
            Long expiry = expiries.get(id);
            if (expiry != null && System.currentTimeMillis() >= expiry) {
                expiries.remove(id);
                save();
            }
        });

        Lifesteal.LOGGER.info("[Grace] Manager registered.");
    }

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    /** Starts the window for a player who has just respawned from a death. */
    public static void beginAfterDeath(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        save();

        player.sendSystemMessage(Component.literal(
                "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                        + ". Wearing armor or attacking another player will end the grace period."
        ).withStyle(ChatFormatting.YELLOW));
    }

    /**
     * Whether the player is currently protected. Expiry is settled here rather than only on the
     * scan, so protection never outlives its deadline by up to a second of scan latency.
     */
    public static boolean isProtected(ServerPlayer player) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return false;

        Long expiry = expiries.get(player.getUUID());
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            end(player);
            return false;
        }
        return true;
    }

    /** Ends the window and tells the player, if they had one. The reason is never given. */
    public static void end(ServerPlayer player) {
        if (expiries.remove(player.getUUID()) == null) return;
        lastNotice.remove(player.getUUID());
        save();
        player.sendSystemMessage(
                Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    /** Renders a duration the way a player would say it: "30 minutes", "1 minute 30 seconds". */
    public static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60 && seconds % 60 == 0)     return plural(seconds / 60, "minute");
        if (seconds < 60)                           return plural(seconds, "second");
        return plural(seconds / 60, "minute") + " " + plural(seconds % 60, "second");
    }

    private static String plural(int value, String unit) {
        return value + " " + unit + (value == 1 ? "" : "s");
    }

    private static void onTick(MinecraftServer server) {
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;
        if (expiries.isEmpty()) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isProtected(player)) continue;
            if (isWearingArmor(player)) end(player);
        }
    }

    /**
     * Cancels player-initiated damage against a protected victim, and burns the attacker's own
     * protection for having thrown the punch — whether or not it landed. Damage from mobs, the
     * world, or the player themselves is untouched.
     */
    private static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!ServerConfig.getInstance().gracePeriodEnabled) return true;
        if (!(entity instanceof ServerPlayer victim)) return true;
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == victim) return true;

        end(attacker);

        if (!isProtected(victim)) return true;

        notifyAttacker(attacker, victim);
        return false;
    }

    /** Throttled so that a bow volley or a held swing does not flood the attacker's chat. */
    private static void notifyAttacker(ServerPlayer attacker, ServerPlayer victim) {
        long now = System.currentTimeMillis();
        Long last = lastNotice.get(attacker.getUUID());
        if (last != null && now - last < NOTICE_INTERVAL_MS) return;

        lastNotice.put(attacker.getUUID(), now);
        attacker.sendSystemMessage(Component.literal(
                victim.getName().getString() + " is still in their grace period."
        ).withStyle(ChatFormatting.YELLOW));
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            Map<UUID, Long> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                expiries.clear();
                expiries.putAll(loaded);
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
