package nightfallmods.lifesteal.mixin;

import com.google.gson.JsonObject;
import nightfallmods.lifesteal.Lifesteal;
import nightfallmods.lifesteal.config.CustomRecipeLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Shadow @Final private HolderLookup.Provider registries;

    @Shadow
    protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> key, JsonObject json, HolderLookup.Provider registries) {
        throw new AssertionError();
    }

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/world/item/crafting/RecipeMap;",
            at = @At("RETURN"), cancellable = true)
    private void lifesteal$injectConfigRecipes(ResourceManager resourceManager, ProfilerFiller profiler,
                                               CallbackInfoReturnable<RecipeMap> cir) {
        Map<String, JsonObject> configured = CustomRecipeLoader.loadConfigured();
        if (configured.isEmpty()) return;

        List<RecipeHolder<?>> merged = new ArrayList<>(cir.getReturnValue().values());
        int added = 0;

        for (Map.Entry<String, JsonObject> entry : configured.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null) {
                Lifesteal.LOGGER.error("[Recipes] Invalid recipe id '{}' — skipped.", entry.getKey());
                continue;
            }
            try {
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
                RecipeHolder<?> holder = fromJson(key, entry.getValue(), this.registries);
                merged.removeIf(existing -> existing.id().equals(key));
                merged.add(holder);
                added++;
            } catch (Exception e) {
                Lifesteal.LOGGER.error("[Recipes] Failed to parse recipe {} — skipped. ({})", id, e.getMessage());
            }
        }

        if (added > 0) {
            cir.setReturnValue(RecipeMap.create(merged));
            Lifesteal.LOGGER.info("[Recipes] Loaded {} custom recipe(s) from config.", added);
        }
    }
}

