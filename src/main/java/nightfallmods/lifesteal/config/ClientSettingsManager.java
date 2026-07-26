package nightfallmods.lifesteal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientSettingsManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "lifesteal-client.json";

    private static ClientSettingsManager instance;

    public boolean playSounds              = true;
    public boolean showHeartChangeMessages = true;
    public boolean showReviveNotifications = true;

    public static ClientSettingsManager getInstance() {
        if (instance == null) instance = load();
        return instance;
    }

    private static ClientSettingsManager load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                ClientSettingsManager cfg = GSON.fromJson(reader, ClientSettingsManager.class);
                if (cfg != null) return cfg;
            } catch (IOException | JsonParseException e) {
                Lifesteal.LOGGER.error("[ClientConfig] Failed to load — using defaults. ({})", e.getMessage());
            }
        }
        ClientSettingsManager defaults = new ClientSettingsManager();
        defaults.save();
        return defaults;
    }

    public void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (FileWriter writer = new FileWriter(path.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[ClientConfig] Failed to save. ({})", e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public void toggleSounds()              { playSounds              = !playSounds;              save(); }
    public void toggleHeartChangeMessages() { showHeartChangeMessages = !showHeartChangeMessages; save(); }
    public void toggleReviveNotifications() { showReviveNotifications = !showReviveNotifications; save(); }
}
