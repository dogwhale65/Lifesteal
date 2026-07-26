package nightfallmods.lifesteal.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.CustomRecipeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * RecipeMap and the ResourceKey-based fromJson arrive in 1.21.2. On this version RecipeManager is a
 * plain JSON reload listener, so the configured recipes are merged into the raw JSON map before
 * vanilla parses it — config entries override same-id datapack and vanilla recipes.
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

        if (added > 0) {
            Lifesteal.LOGGER.info("[Recipes] Loaded {} custom recipe(s) from config.", added);
        }
        return merged;
    }

    /**
     * Vanilla logs a recipe *type* count, and a recipe whose JSON vanilla rejects is only reported
     * at debug level — so confirm each configured recipe actually reached the recipe manager.
     */
    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN"))
    private void lifesteal$verifyConfigRecipes(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
                                               ProfilerFiller profiler, CallbackInfo ci) {
        RecipeManager self = (RecipeManager) (Object) this;

        for (String key : CustomRecipeLoader.loadConfigured().keySet()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            if (self.byKey(id).isEmpty()) {
                Lifesteal.LOGGER.error("[Recipes] Recipe {} was rejected by the game — check its JSON.", id);
            }
        }
    }
}

