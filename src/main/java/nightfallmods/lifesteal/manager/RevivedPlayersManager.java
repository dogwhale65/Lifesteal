package nightfallmods.lifesteal.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.ServerConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RevivedPlayersManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "lifesteal-revived.json";
    private static final Set<UUID> revivedPlayers = new HashSet<>();
    private static Path dataPath;

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!revivedPlayers.contains(player.getUUID())) return;

            applyReviveHearts(player);

            player.setGameMode(GameType.SURVIVAL);
            revivedPlayers.remove(player.getUUID());
            save();
            Lifesteal.LOGGER.info("[Revival] Applied revive hearts to {} on join.", player.getName().getString());
        });
    }

    public static void setDataPath(Path dir) {
        dataPath = dir.resolve(DATA_FILE);
        load();
    }

    public static void markRevived(UUID uuid) {
        revivedPlayers.add(uuid);
        save();
    }

    public static boolean isRevived(UUID uuid) {
        return revivedPlayers.contains(uuid);
    }

    private static void applyReviveHearts(ServerPlayer player) {
        double health = ServerConfig.getInstance().getReviveHealth();
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.setBaseValue(health);
        player.setHealth((float) health);
        CraftedHeartTracker.reset(player.getUUID());
    }

    private static void load() {
        if (dataPath == null || !Files.exists(dataPath)) return;
        try (FileReader reader = new FileReader(dataPath.toFile())) {
            Type type = new TypeToken<HashSet<UUID>>(){}.getType();
            Set<UUID> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                revivedPlayers.clear();
                revivedPlayers.addAll(loaded);
                Lifesteal.LOGGER.info("[Revival] Loaded {} pending revive(s).", revivedPlayers.size());
            }
        } catch (IOException | JsonParseException e) {
            Lifesteal.LOGGER.error("[Revival] Failed to load pending revivals. ({})", e.getMessage());
        }
    }

    private static void save() {
        if (dataPath == null) return;
        try {
            Files.createDirectories(dataPath.getParent());
            try (FileWriter writer = new FileWriter(dataPath.toFile())) {
                GSON.toJson(revivedPlayers, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Revival] Failed to save pending revivals. ({})", e.getMessage());
        }
    }
}
