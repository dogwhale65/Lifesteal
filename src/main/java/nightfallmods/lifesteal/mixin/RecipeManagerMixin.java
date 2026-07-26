package nightfallmods.lifesteal.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.CustomRecipeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Map;

/**
 * RecipeMap and the ResourceKey-based fromJson do not exist yet on this version: the recipe manager
 * is still a plain SimpleJsonResourceReloadListener that gets handed the raw JSON of every recipe.
 * Rather than parsing the configured recipes ourselves, the config JSON is merged into that map at
 * the head of apply() and vanilla parses it exactly like a datapack recipe — same ids override.
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Map<ResourceLocation, JsonElement> lifesteal$injectConfigRecipes(Map<ResourceLocation, JsonElement> map) {
        Map<String, JsonObject> configured = CustomRecipeLoader.loadConfigured();
        if (configured.isEmpty()) return map;

        Map<ResourceLocation, JsonElement> merged = new HashMap<>(map);
        int added = 0;

        for (Map.Entry<String, JsonObject> entry : configured.entrySet()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            if (id == null) {
                Lifesteal.LOGGER.error("[Recipes] Invalid recipe id '{}' — skipped.", entry.getKey());
                continue;
            }
            merged.put(id, entry.getValue());
            added++;
        }

        if (added == 0) return map;

        Lifesteal.LOGGER.info("[Recipes] Loaded {} custom recipe(s) from config.", added);
        return merged;
    }
}
