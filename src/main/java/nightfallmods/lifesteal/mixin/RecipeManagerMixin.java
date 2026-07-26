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
 * This version's {@link RecipeManager} is still a plain JSON reload listener, so config recipes
 * are merged into the raw id-to-JSON map and parsed by vanilla — no {@code RecipeMap} and no
 * {@code fromJson} call of our own. Config entries override datapack/vanilla recipes with the
 * same id.
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @ModifyVariable(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true)
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

        if (added > 0) {
            Lifesteal.LOGGER.info("[Recipes] Loaded {} custom recipe(s) from config.", added);
        }
        return merged;
    }
}
