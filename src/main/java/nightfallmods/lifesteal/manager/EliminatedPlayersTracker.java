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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EliminatedPlayersTracker {

    public record Entry(String name, long eliminatedAt, boolean banned) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-eliminated.json";

    private static final Map<UUID, Entry> eliminated = new HashMap<>();
    private static Path dataPath;

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    public static void markEliminated(UUID uuid, String name, boolean banned) {
        eliminated.put(uuid, new Entry(name, System.currentTimeMillis(), banned));
        save();
    }

    public static void clearEliminated(UUID uuid) {
        if (eliminated.remove(uuid) != null) save();
    }

    public static boolean isEliminated(UUID uuid) {
        return eliminated.containsKey(uuid);
    }

    public static List<Entry> getEntries() {
        return List.copyOf(eliminated.values());
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashMap<UUID, Entry>>(){}.getType();
            Map<UUID, Entry> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                eliminated.clear();
                eliminated.putAll(loaded);
                Lifesteal.LOGGER.info("[Eliminated] Loaded {} eliminated player(s).", eliminated.size());
            }
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[Eliminated] Failed to load eliminated players. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(eliminated, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Eliminated] Failed to save eliminated players. ({})", e.getMessage());
        }
    }
}

