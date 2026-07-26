package nightfallmods.lifesteal.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import nightfallmods.lifesteal.Lifesteal;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomRecipeLoader {

    private static final String DIR_NAME = "lifesteal-recipes";

    // 1.20.1 ingredients must be objects ("Expected item to be object or array of objects"), and
    // recipe results are keyed "item" rather than the "id" form introduced in 1.20.5.
    private static final String DEFAULT_HEART_FRAGMENT = """
            {
              "type": "minecraft:crafting_shaped",
              "pattern": [
                "sds",
                "dnd",
                "sds"
              ],
              "key": {
                "n": {
                  "item": "minecraft:netherite_ingot"
                },
                "d": {
                  "item": "minecraft:diamond_block"
                },
                "s": {
                  "item": "minecraft:netherite_scrap"
                }
              },
              "result": {
                "item": "lifesteal:heart_fragment",
                "count": 1
              }
            }
            """;

    private static final String DEFAULT_CRAFTED_HEART = """
            {
              "type": "minecraft:crafting_shaped",
              "pattern": [
                "fnf",
                "ntn",
                "fnf"
              ],
              "key": {
                "n": {
                  "item": "minecraft:netherite_ingot"
                },
                "f": {
                  "item": "lifesteal:heart_fragment"
                },
                "t": {
                  "item": "minecraft:totem_of_undying"
                }
              },
              "result": {
                "item": "lifesteal:crafted_heart",
                "count": 1
              }
            }
            """;

    private static final String DEFAULT_BEACON_OF_LIFE = """
            {
              "type": "minecraft:crafting_shaped",
              "pattern": [
                "hhh",
                "hrh",
                "nnn"
              ],
              "key": {
                "h": {
                  "item": "lifesteal:heart"
                },
                "r": {
                  "item": "minecraft:recovery_compass"
                },
                "n": {
                  "item": "minecraft:netherite_ingot"
                }
              },
              "result": {
                "item": "lifesteal:beacon_of_life",
                "count": 1
              }
            }
            """;

    public static Path recipeDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR_NAME);
    }

    public static void writeDefaultTemplates() {
        try {
            Files.createDirectories(recipeDir());
            writeIfAbsent("heart_fragment.json", DEFAULT_HEART_FRAGMENT);
            writeIfAbsent("crafted_heart.json", DEFAULT_CRAFTED_HEART);
            writeIfAbsent("beacon_of_life.json", DEFAULT_BEACON_OF_LIFE);
        } catch (IOException e) {
            Lifesteal.LOGGER.error("[Recipes] Failed to write default recipe templates. ({})", e.getMessage());
        }
    }

    public static Map<String, JsonObject> loadConfigured() {
        Map<String, JsonObject> recipes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : ServerConfig.getInstance().customRecipes.entrySet()) {
            Path file = recipeDir().resolve(entry.getValue());
            if (!Files.exists(file)) {
                Lifesteal.LOGGER.warn("[Recipes] Missing recipe file {} for {} — skipped.", entry.getValue(), entry.getKey());
                continue;
            }
            try (FileReader reader = new FileReader(file.toFile())) {
                recipes.put(entry.getKey(), JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | JsonParseException | IllegalStateException e) {
                Lifesteal.LOGGER.error("[Recipes] Failed to read {} — skipped. ({})", entry.getValue(), e.getMessage());
            }
        }
        return recipes;
    }

    private static void writeIfAbsent(String name, String content) throws IOException {
        Path file = recipeDir().resolve(name);
        if (!Files.exists(file)) Files.writeString(file, content);
    }
}

