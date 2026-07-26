package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftedHeartTracker {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-crafted-hearts.json";

    private static final Map<UUID, Integer> counts = new HashMap<>();
    private static Path dataPath;

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    public static int getCount(UUID player) {
        return counts.getOrDefault(player, 0);
    }

    public static void increment(UUID player) {
        counts.merge(player, 1, Integer::sum);
        save();
    }

    public static void take(UUID player, int amount) {
        if (amount <= 0) return;
        int remaining = Math.max(0, getCount(player) - amount);
        if (remaining == 0) counts.remove(player);
        else counts.put(player, remaining);
        save();
    }

    public static void clampTo(UUID player, int maxHearts) {
        int current = getCount(player);
        if (current <= maxHearts) return;
        if (maxHearts <= 0) counts.remove(player);
        else counts.put(player, maxHearts);
        save();
    }

    public static void reset(UUID player) {
        if (counts.remove(player) != null) save();
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Integer>>(){}.getType();
            Map<UUID, Integer> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                counts.clear();
                counts.putAll(loaded);
            }
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[CraftedHearts] Failed to load tally. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(counts, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[CraftedHearts] Failed to save tally. ({})", e.getMessage());
        }
    }
}

