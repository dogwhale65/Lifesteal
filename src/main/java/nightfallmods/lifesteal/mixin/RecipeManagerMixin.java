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
 * 1.20.1 has no RecipeMap and no RecipeHolder — RecipeManager reloads from a raw
 * Map&lt;ResourceLocation, JsonElement&gt;. Merging the configured JSON into that map before
 * vanilla parses it needs no serializer access at all, and overrides any datapack entry
 * sharing the same id.
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true)
    private Map<ResourceLocation, JsonElement> lifesteal$injectConfigRecipes(Map<ResourceLocation, JsonElement> recipes) {
        Map<String, JsonObject> configured = CustomRecipeLoader.loadConfigured();
        if (configured.isEmpty()) return recipes;

        Map<ResourceLocation, JsonElement> merged = new HashMap<>(recipes);
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

        if (added > 0) {
            Lifesteal.LOGGER.info("[Recipes] Loaded {} custom recipe(s) from config.", added);
        }
        return merged;
    }
}
