package nightfallmods.lifesteal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EnchantmentsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "lifesteal-enchantments.json";

    private static EnchantmentsConfig instance;

    public boolean enforce = true;

    public Map<String, Integer> maxLevels = new TreeMap<>();

    public List<String> disabled = new ArrayList<>();

    public static EnchantmentsConfig getInstance() {
        if (instance == null) instance = new EnchantmentsConfig();
        return instance;
    }

    // Enchantments only become data-driven in 1.21; on 1.20.1 they live in the static registry.
    public static void init() {
        EnchantmentsConfig cfg = load();
        Registry<Enchantment> reg = BuiltInRegistries.ENCHANTMENT;

        boolean added = false;
        for (Enchantment enchant : reg.stream().toList()) {
            ResourceLocation id = reg.getKey(enchant);
            if (id == null) continue;
            if (cfg.maxLevels.putIfAbsent(id.toString(), enchant.getMaxLevel()) == null) added = true;
        }
        if (added) cfg.save();

        instance = cfg;
        Lifesteal.LOGGER.info("[Enchants] Limits active for {} enchantment(s), {} disabled, enforce={}.",
                cfg.maxLevels.size(), cfg.disabled.size(), cfg.enforce);
    }

    public int maxLevelFor(String enchantId, int vanillaMax) {
        Integer configured = maxLevels.get(enchantId);
        return configured == null ? vanillaMax : configured;
    }

    public boolean isDisabled(String enchantId) {
        return disabled.contains(enchantId);
    }

    private static EnchantmentsConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                EnchantmentsConfig cfg = GSON.fromJson(reader, EnchantmentsConfig.class);
                if (cfg != null) {
                    if (cfg.maxLevels == null) cfg.maxLevels = new TreeMap<>();
                    else cfg.maxLevels = new TreeMap<>(cfg.maxLevels);
                    if (cfg.disabled == null) cfg.disabled = new ArrayList<>();
                    return cfg;
                }
            } catch (IOException | JsonParseException e) {
                Lifesteal.LOGGER.error("[Enchants] Failed to load — using defaults. ({})", e.getMessage());
            }
        }
        return new EnchantmentsConfig();
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (FileWriter writer = new FileWriter(path.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Enchants] Failed to save. ({})", e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }
}

