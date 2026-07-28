package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Post-death grace period: while it runs the player takes no player-initiated damage at all.
 * It ends when the timer runs out, when they put armour on, or when they swing at another player.
 */
public class GracePeriodManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-grace.json";
    private static final int SCAN_INTERVAL_TICKS = 20;

    /** Player -> epoch millis the protection lapses. */
    private static final Map<UUID, Long> expiries = new HashMap<>();

    private static Path dataPath;
    private static int tickCounter = 0;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            dataPath = FabricLoader.getInstance().getConfigDir().resolve(DATA_FILE);
            load();
        });

        // alive == true means an End-portal return rather than a death, which grants nothing.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) start(newPlayer);
        });

        ServerTickEvents.END_SERVER_TICK.register(GracePeriodManager::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(GracePeriodManager::allowDamage);

        Lifesteal.LOGGER.info("[Grace] Manager registered.");
    }

    public static boolean isProtected(Player player) {
        Long expiry = expiries.get(player.getUUID());
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    private static void start(ServerPlayer player) {
        ServerConfig cfg = ServerConfig.getInstance();
        if (!cfg.gracePeriodEnabled || cfg.gracePeriodSeconds <= 0) return;

        expiries.put(player.getUUID(), System.currentTimeMillis() + cfg.gracePeriodSeconds * 1000L);
        save();

        player.sendSystemMessage(Component.literal(
                "You are protected from other players for " + describe(cfg.gracePeriodSeconds)
                        + ". Wearing armor or attacking another player will end the grace period.")
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void end(ServerPlayer player) {
        if (expiries.remove(player.getUUID()) == null) return;
        save();
        player.sendSystemMessage(Component.literal("Your grace period ended.").withStyle(ChatFormatting.RED));
    }

    private static void onTick(MinecraftServer server) {
        if (expiries.isEmpty()) return;
        if (++tickCounter < SCAN_INTERVAL_TICKS) return;
        tickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!expiries.containsKey(player.getUUID())) continue;
            if (!isProtected(player) || isWearingArmor(player)) end(player);
        }

        // Anyone whose timer lapsed while offline just loses the entry — no message to deliver.
        boolean pruned = false;
        for (Iterator<Map.Entry<UUID, Long>> it = expiries.entrySet().iterator(); it.hasNext(); ) {
            if (System.currentTimeMillis() >= it.next().getValue()) { it.remove(); pruned = true; }
        }
        if (pruned) save();
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        ServerPlayer attacker = playerBehind(source);
        if (attacker == null) return true;

        // Swinging at another player forfeits your own protection, hit landed or not.
        if (entity instanceof Player && entity != attacker) end(attacker);

        if (!(entity instanceof ServerPlayer victim) || victim == attacker) return true;
        if (!isProtected(victim)) return true;

        // Action bar: this fires on every swing, and chat would drown the attacker in it.
        attacker.sendSystemMessage(Component.literal(
                victim.getName().getString() + " is still in their grace period.")
                .withStyle(ChatFormatting.YELLOW), true);
        return false;
    }

    /** The player responsible for a damage source — the shooter for projectiles, not the arrow. */
    private static ServerPlayer playerBehind(DamageSource source) {
        Entity owner = source.getEntity();
        if (owner instanceof ServerPlayer sp) return sp;
        Entity direct = source.getDirectEntity();
        return direct instanceof ServerPlayer sp ? sp : null;
    }

    private static boolean isWearingArmor(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof ArmorItem) return true;
        }
        return false;
    }

    /** "30 minutes", "2 hours", "45 seconds" — whichever unit divides the duration cleanly. */
    public static String describe(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) return plural(seconds / 3600, "hour");
        if (seconds >= 60 && seconds % 60 == 0)     return plural(seconds / 60, "minute");
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
