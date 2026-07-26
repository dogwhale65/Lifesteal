package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.manager.UniqueItemManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CrafterBlock.class)
public class CrafterBlockMixin {

    @Redirect(
            method = "dispenseFrom",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack lifesteal$gateCrafterUnique(CraftingRecipe recipe, RecipeInput input, HolderLookup.Provider registries) {

        ItemStack result = recipe.assemble((CraftingInput) input, registries);
        if (UniqueItemManager.blocksCrafting(result)) return ItemStack.EMPTY;
        UniqueItemManager.tagCrafted(result);
        return result;
    }
}

