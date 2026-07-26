package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatCooldownManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-cooldowns.json";
    private static final ZoneId CST = ZoneId.of("America/Chicago");

    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static Path dataPath;

    public static void register(Path configDir) {
        dataPath = configDir.resolve(DATA_FILE);
        load();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID id = handler.getPlayer().getUUID();
            if (cooldowns.containsKey(id) && System.currentTimeMillis() >= cooldowns.get(id)) {
                cooldowns.remove(id);
                save();
            }
        });

        Lifesteal.LOGGER.info("[Cooldowns] Manager registered.");
    }

    public static void applyCooldown(UUID playerId) {
        long expiry = nextNoonOrMidnightCST();
        cooldowns.put(playerId, expiry);
        save();
        Lifesteal.LOGGER.info("[Cooldowns] Cooldown applied to {} until {}.", playerId, expiry);
    }

    public static boolean isOnCooldown(UUID playerId) {
        Long expiry = cooldowns.get(playerId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(playerId);
            save();
            return false;
        }
        return true;
    }

    public static long getCooldownExpiry(UUID playerId) {
        return cooldowns.getOrDefault(playerId, 0L);
    }

    private static long nextNoonOrMidnightCST() {
        ZonedDateTime now       = ZonedDateTime.now(CST);
        ZonedDateTime nextNoon  = now.toLocalDate().atStartOfDay(CST).plusHours(12);
        ZonedDateTime nextMidnight = now.toLocalDate().atStartOfDay(CST).plusDays(1);
        return (now.getHour() >= 12 ? nextMidnight : nextNoon).toInstant().toEpochMilli();
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Long>>(){}.getType();
            Map<UUID, Long> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                cooldowns.clear();
                cooldowns.putAll(loaded);
                Lifesteal.LOGGER.info("[Cooldowns] Loaded {} active cooldown(s).", cooldowns.size());
            }
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[Cooldowns] Failed to load. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(cooldowns, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Cooldowns] Failed to save. ({})", e.getMessage());
        }
    }
}
